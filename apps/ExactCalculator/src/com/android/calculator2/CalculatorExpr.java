/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;


import com.hp.creals.CR;
import com.hp.creals.UnaryCRFunction;

import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TtsSpan;
import android.text.style.TtsSpan.TextBuilder;
import android.util.Log;

import java.math.BigInteger;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

// A mathematical expression represented as a sequence of "tokens".
// Many tokes are represented by button ids for the corresponding operator.
// Parsed only when we evaluate the expression using the "eval" method.
class CalculatorExpr {
    private ArrayList<Token> mExpr;  // The actual representation
                                     // as a list of tokens.  Constant
                                     // tokens are always nonempty.

    private static enum TokenKind { CONSTANT, OPERATOR, PRE_EVAL };
    private static TokenKind[] tokenKindValues = TokenKind.values();
    private final static BigInteger BIG_MILLION = BigInteger.valueOf(1000000);
    private final static BigInteger BIG_BILLION = BigInteger.valueOf(1000000000);

    private static abstract class Token {
        abstract TokenKind kind();

        /**
         * Write kind as Byte followed by data needed by subclass constructor.
         */
        abstract void write(DataOutput out) throws IOException;

        /**
         * Return a textual representation of the token.
         * The result is suitable for either display as part od the formula or TalkBack use.
         * It may be a SpannableString that includes added TalkBack information.
         * @param context context used for converting button ids to strings
         */
        abstract CharSequence toCharSequence(Context context);
    }

    // An operator token
    private static class Operator extends Token {
	final int mId; // We use the button resource id
        Operator(int resId) {
	    mId = resId;
        }
        Operator(DataInput in) throws IOException {
            mId = in.readInt();
        }
        @Override
        void write(DataOutput out) throws IOException {
            out.writeByte(TokenKind.OPERATOR.ordinal());
            out.writeInt(mId);
        }
        @Override
        public CharSequence toCharSequence(Context context) {
            String desc = KeyMaps.toDescriptiveString(context, mId);
            if (desc != null) {
                SpannableString result = new SpannableString(KeyMaps.toString(context, mId));
                Object descSpan = new TtsSpan.TextBuilder(desc).build();
                result.setSpan(descSpan, 0, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return result;
            } else {
                return KeyMaps.toString(context, mId);
            }
        }
        @Override
        TokenKind kind() { return TokenKind.OPERATOR; }
    }

    // A (possibly incomplete) numerical constant.
    // Supports addition and removal of trailing characters; hence mutable.
    private static class Constant extends Token implements Cloneable {
        private boolean mSawDecimal;
        String mWhole;  // String preceding decimal point.
        private String mFraction; // String after decimal point.
        private int mExponent;  // Explicit exponent, only generated through addExponent.

        Constant() {
            mWhole = "";
            mFraction = "";
            mSawDecimal = false;
            mExponent = 0;
        };

        Constant(DataInput in) throws IOException {
            mWhole = in.readUTF();
            mSawDecimal = in.readBoolean();
            mFraction = in.readUTF();
            mExponent = in.readInt();
        }

        @Override
        void write(DataOutput out) throws IOException {
            out.writeByte(TokenKind.CONSTANT.ordinal());
            out.writeUTF(mWhole);
            out.writeBoolean(mSawDecimal);
            out.writeUTF(mFraction);
            out.writeInt(mExponent);
        }

        // Given a button press, append corresponding digit.
        // We assume id is a digit or decimal point.
        // Just return false if this was the second (or later) decimal point
        // in this constant.
        // Assumes that this constant does not have an exponent.
        boolean add(int id) {
            if (id == R.id.dec_point) {
                if (mSawDecimal || mExponent != 0) return false;
                mSawDecimal = true;
                return true;
            }
            int val = KeyMaps.digVal(id);
            if (mExponent != 0) {
                if (Math.abs(mExponent) <= 10000) {
                    if (mExponent > 0) {
                        mExponent = 10 * mExponent + val;
                    } else {
                        mExponent = 10 * mExponent - val;
                    }
                    return true;
                } else {  // Too large; refuse
                    return false;
                }
            }
            if (mSawDecimal) {
                mFraction += val;
            } else {
                mWhole += val;
            }
            return true;
        }

        void addExponent(int exp) {
            // Note that adding a 0 exponent is a no-op.  That's OK.
            mExponent = exp;
        }

        // Undo the last add.
        // Assumes the constant is nonempty.
        void delete() {
            if (mExponent != 0) {
                mExponent /= 10;
                // Once zero, it can only be added back with addExponent.
            } else if (!mFraction.isEmpty()) {
                mFraction = mFraction.substring(0, mFraction.length() - 1);
            } else if (mSawDecimal) {
                mSawDecimal = false;
            } else {
                mWhole = mWhole.substring(0, mWhole.length() - 1);
            }
        }

        boolean isEmpty() {
            return (mSawDecimal == false && mWhole.isEmpty());
        }

        // Produces human-readable string, as typed.
        // Result is internationalized.
        @Override
        public String toString() {
            String result = mWhole;
            if (mSawDecimal) {
                result += '.';
                result += mFraction;
            }
            if (mExponent != 0) {
                result += "E" + mExponent;
            }
            return KeyMaps.translateResult(result);
        }

        // Return non-null BoundedRational representation.
        public BoundedRational toRational() {
            String whole = mWhole;
            if (whole.isEmpty()) whole = "0";
            BigInteger num = new BigInteger(whole + mFraction);
            BigInteger den = BigInteger.TEN.pow(mFraction.length());
            if (mExponent > 0) {
                num = num.multiply(BigInteger.TEN.pow(mExponent));
            }
            if (mExponent < 0) {
                den = den.multiply(BigInteger.TEN.pow(-mExponent));
            }
            return new BoundedRational(num, den);
        }

        @Override
        CharSequence toCharSequence(Context context) {
            return toString();
        }

        @Override
        TokenKind kind() { return TokenKind.CONSTANT; }

        // Override clone to make it public
        @Override
        public Object clone() {
            Constant res = new Constant();
            res.mWhole = mWhole;
            res.mFraction = mFraction;
            res.mSawDecimal = mSawDecimal;
            res.mExponent = mExponent;
            return res;
        }
    }

    // Hash maps used to detect duplicate subexpressions when
    // we write out CalculatorExprs and read them back in.
    private static final ThreadLocal<IdentityHashMap<CR,Integer>>outMap =
                new ThreadLocal<IdentityHashMap<CR,Integer>>();
        // Maps expressions to indices on output
    private static final ThreadLocal<HashMap<Integer,PreEval>>inMap =
                new ThreadLocal<HashMap<Integer,PreEval>>();
        // Maps expressions to indices on output
    private static final ThreadLocal<Integer> exprIndex =
                new ThreadLocal<Integer>();

    static void initExprOutput() {
        outMap.set(new IdentityHashMap<CR,Integer>());
        exprIndex.set(Integer.valueOf(0));
    }

    static void initExprInput() {
        inMap.set(new HashMap<Integer,PreEval>());
    }

    // We treat previously evaluated subexpressions as tokens
    // These are inserted when either:
    //    - We continue an expression after evaluating some of it.
    //    - TODO: When we copy/paste expressions.
    // The representation includes three different representations
    // of the expression:
    //  1) The CR value for use in computation.
    //  2) The integer value for use in the computations,
    //     if the expression evaluates to an integer.
    //  3a) The corresponding CalculatorExpr, together with
    //  3b) The context (currently just deg/rad mode) used to evaluate
    //      the expression.
    //  4) A short string representation that is used to
    //     Display the expression.
    //
    // (3) is present only so that we can persist the object.
    // (4) is stored explicitly to avoid waiting for recomputation in the UI
    //       thread.
    private static class PreEval extends Token {
        final CR mValue;
        final BoundedRational mRatValue;
        private final CalculatorExpr mExpr;
        private final EvalContext mContext;
        private final String mShortRep;  // Not internationalized.
        PreEval(CR val, BoundedRational ratVal, CalculatorExpr expr,
                EvalContext ec, String shortRep) {
            mValue = val;
            mRatValue = ratVal;
            mExpr = expr;
            mContext = ec;
            mShortRep = shortRep;
        }
        // In writing out PreEvals, we are careful to avoid writing
        // out duplicates.  We assume that two expressions are
        // duplicates if they have the same mVal.  This avoids a
        // potential exponential blow up in certain off cases and
        // redundant evaluation after reading them back in.
        // The parameter hash map maps expressions we've seen
        // before to their index.
        @Override
        void write(DataOutput out) throws IOException {
            out.writeByte(TokenKind.PRE_EVAL.ordinal());
            Integer index = outMap.get().get(mValue);
            if (index == null) {
                int nextIndex = exprIndex.get() + 1;
                exprIndex.set(nextIndex);
                outMap.get().put(mValue, nextIndex);
                out.writeInt(nextIndex);
                mExpr.write(out);
                mContext.write(out);
                out.writeUTF(mShortRep);
            } else {
                // Just write out the index
                out.writeInt(index);
            }
        }
        PreEval(DataInput in) throws IOException {
            int index = in.readInt();
            PreEval prev = inMap.get().get(index);
            if (prev == null) {
                mExpr = new CalculatorExpr(in);
                mContext = new EvalContext(in, mExpr.mExpr.size());
                // Recompute other fields
                // We currently do this in the UI thread, but we
                // only create PreEval expressions that were
                // previously successfully evaluated, and thus
                // don't diverge.  We also only evaluate to a
                // constructive real, which involves substantial
                // work only in fairly contrived circumstances.
                // TODO: Deal better with slow evaluations.
                EvalRet res = null;
                try {
                    res = mExpr.evalExpr(0, mContext);
                } catch (SyntaxException e) {
                    // Should be impossible, since we only write out
                    // expressions that can be evaluated.
                    Log.e("Calculator", "Unexpected syntax exception" + e);
                }
                mValue = res.mVal;
                mRatValue = res.mRatVal;
                mShortRep = in.readUTF();
                inMap.get().put(index, this);
            } else {
                mValue = prev.mValue;
                mRatValue = prev.mRatValue;
                mExpr = prev.mExpr;
                mContext = prev.mContext;
                mShortRep = prev.mShortRep;
            }
        }
        @Override
        CharSequence toCharSequence(Context context) {
            return KeyMaps.translateResult(mShortRep);
        }
        @Override
        TokenKind kind() {
            return TokenKind.PRE_EVAL;
        }
        boolean hasEllipsis() {
            return mShortRep.lastIndexOf(KeyMaps.ELLIPSIS) != -1;
        }
    }

    static Token newToken(DataInput in) throws IOException {
        TokenKind kind = tokenKindValues[in.readByte()];
        switch(kind) {
        case CONSTANT:
            return new Constant(in);
        case OPERATOR:
            return new Operator(in);
        case PRE_EVAL:
            return new PreEval(in);
        default: throw new IOException("Bad save file format");
        }
    }

    CalculatorExpr() {
        mExpr = new ArrayList<Token>();
    }

    private CalculatorExpr(ArrayList<Token> expr) {
        mExpr = expr;
    }

    CalculatorExpr(DataInput in) throws IOException {
        mExpr = new ArrayList<Token>();
        int size = in.readInt();
        for (int i = 0; i < size; ++i) {
            mExpr.add(newToken(in));
        }
    }

    void write(DataOutput out) throws IOException {
        int size = mExpr.size();
        out.writeInt(size);
        for (int i = 0; i < size; ++i) {
            mExpr.get(i).write(out);
        }
    }

    boolean hasTrailingConstant() {
        int s = mExpr.size();
        if (s == 0) {
            return false;
        }
        Token t = mExpr.get(s-1);
        return t instanceof Constant;
    }

    private boolean hasTrailingBinary() {
        int s = mExpr.size();
        if (s == 0) return false;
        Token t = mExpr.get(s-1);
        if (!(t instanceof Operator)) return false;
        Operator o = (Operator)t;
        return (KeyMaps.isBinary(o.mId));
    }

    /**
     * Append press of button with given id to expression.
     * If the insertion would clearly result in a syntax error, either just return false
     * and do nothing, or make an adjustment to avoid the problem.  We do the latter only
     * for unambiguous consecutive binary operators, in which case we delete the first
     * operator.
     */
    boolean add(int id) {
        int s = mExpr.size();
        int d = KeyMaps.digVal(id);
        boolean binary = KeyMaps.isBinary(id);
        Token lastTok = s == 0 ? null : mExpr.get(s-1);
        int lastOp = lastTok instanceof Operator ? ((Operator) lastTok).mId : 0;
        // Quietly replace a trailing binary operator with another one, unless the second
        // operator is minus, in which case we just allow it as a unary minus.
        if (binary && !KeyMaps.isPrefix(id)) {
            if (s == 0 || lastOp == R.id.lparen || KeyMaps.isFunc(lastOp)
                    || KeyMaps.isPrefix(lastOp) && lastOp != R.id.op_sub) {
                return false;
            }
            while (hasTrailingBinary()) {
                delete();
            }
            // s invalid and not used below.
        }
        boolean isConstPiece = (d != KeyMaps.NOT_DIGIT || id == R.id.dec_point);
        if (isConstPiece) {
            // Since we treat juxtaposition as multiplication, a constant can appear anywhere.
            if (s == 0) {
                mExpr.add(new Constant());
                s++;
            } else {
                Token last = mExpr.get(s-1);
                if(!(last instanceof Constant)) {
                    if (last instanceof PreEval) {
                        // Add explicit multiplication to avoid confusing display.
                        mExpr.add(new Operator(R.id.op_mul));
                        s++;
                    }
                    mExpr.add(new Constant());
                    s++;
                }
            }
            return ((Constant)(mExpr.get(s-1))).add(id);
        } else {
            mExpr.add(new Operator(id));
            return true;
        }
    }

    /**
     * Add exponent to the constant at the end of the expression.
     * Assumes there is a constant at the end of the expression.
     */
    void addExponent(int exp) {
        Token lastTok = mExpr.get(mExpr.size() - 1);
        ((Constant) lastTok).addExponent(exp);
    }

    /**
     * Remove trailing op_add and op_sub operators.
     */
    void removeTrailingAdditiveOperators() {
        while (true) {
            int s = mExpr.size();
            if (s == 0) break;
            Token lastTok = mExpr.get(s-1);
            if (!(lastTok instanceof Operator)) break;
            int lastOp = ((Operator) lastTok).mId;
            if (lastOp != R.id.op_add && lastOp != R.id.op_sub) break;
            delete();
        }
    }

    // Append the contents of the argument expression.
    // It is assumed that the argument expression will not change,
    // and thus its pieces can be reused directly.
    // TODO: We probably only need this for expressions consisting of
    // a single PreEval "token", and may want to check that.
    void append(CalculatorExpr expr2) {
        // Check that we're not concatenating Constant or PreEval
        // tokens, since the result would look like a single constant
        int s = mExpr.size();
        int s2 = expr2.mExpr.size();
        // Check that we're not concatenating Constant or PreEval
        // tokens, since the result would look like a single constant,
        // with very mysterious results for the user.
        if (s != 0 && s2 != 0) {
            Token last = mExpr.get(s-1);
            Token first = expr2.mExpr.get(0);
            if (!(first instanceof Operator) && !(last instanceof Operator)) {
                // Fudge it by adding an explicit multiplication.
                // We would have interpreted it as such anyway, and this
                // makes it recognizable to the user.
                mExpr.add(new Operator(R.id.op_mul));
            }
        }
        for (int i = 0; i < s2; ++i) {
            mExpr.add(expr2.mExpr.get(i));
        }
    }

    // Undo the last key addition, if any.
    void delete() {
        int s = mExpr.size();
        if (s == 0) return;
        Token last = mExpr.get(s-1);
        if (last instanceof Constant) {
            Constant c = (Constant)last;
            c.delete();
            if (!c.isEmpty()) return;
        }
        mExpr.remove(s-1);
    }

    void clear() {
        mExpr.clear();
    }

    boolean isEmpty() {
        return mExpr.isEmpty();
    }

    // Returns a logical deep copy of the CalculatorExpr.
    // Operator and PreEval tokens are immutable, and thus
    // aren't really copied.
    public Object clone() {
        CalculatorExpr res = new CalculatorExpr();
        for (Token t: mExpr) {
            if (t instanceof Constant) {
                res.mExpr.add((Token)(((Constant)t).clone()));
            } else {
                res.mExpr.add(t);
            }
        }
        return res;
    }

    // Am I just a constant?
    boolean isConstant() {
        if (mExpr.size() != 1) return false;
        return mExpr.get(0) instanceof Constant;
    }

    // Return a new expression consisting of a single PreEval token
    // representing the current expression.
    // The caller supplies the value, degree mode, and short
    // string representation, which must have been previously computed.
    // Thus this is guaranteed to terminate reasonably quickly.
    CalculatorExpr abbreviate(CR val, BoundedRational ratVal,
                              boolean dm, String sr) {
        CalculatorExpr result = new CalculatorExpr();
        Token t = new PreEval(val, ratVal,
                              new CalculatorExpr(
                                        (ArrayList<Token>)mExpr.clone()),
                              new EvalContext(dm, mExpr.size()), sr);
        result.mExpr.add(t);
        return result;
    }

    // Internal evaluation functions return an EvalRet triple.
    // We compute rational (BoundedRational) results when possible, both as
    // a performance optimization, and to detect errors exactly when we can.
    private class EvalRet {
        int mPos; // Next position (expression index) to be parsed
        final CR mVal; // Constructive Real result of evaluating subexpression
        final BoundedRational mRatVal;  // Exact Rational value or null if
                                        // irrational or hard to compute.
        EvalRet(int p, CR v, BoundedRational r) {
            mPos = p;
            mVal = v;
            mRatVal = r;
        }
    }

    // And take a context argument:
    private static class EvalContext {
        public final int mPrefixLength; // Length of prefix to evaluate.
                            // Not explicitly saved.
        public final boolean mDegreeMode;
        // If we add any other kinds of evaluation modes, they go here.
        EvalContext(boolean degreeMode, int len) {
            mDegreeMode = degreeMode;
            mPrefixLength = len;
        }
        EvalContext(DataInput in, int len) throws IOException {
            mDegreeMode = in.readBoolean();
            mPrefixLength = len;
        }
        void write(DataOutput out) throws IOException {
            out.writeBoolean(mDegreeMode);
        }
    }

    private final CR RADIANS_PER_DEGREE = CR.PI.divide(CR.valueOf(180));

    private final CR DEGREES_PER_RADIAN = CR.valueOf(180).divide(CR.PI);

    private CR toRadians(CR x, EvalContext ec) {
        if (ec.mDegreeMode) {
            return x.multiply(RADIANS_PER_DEGREE);
        } else {
            return x;
        }
    }

    private CR fromRadians(CR x, EvalContext ec) {
        if (ec.mDegreeMode) {
            return x.multiply(DEGREES_PER_RADIAN);
        } else {
            return x;
        }
    }

    // The following methods can all throw IndexOutOfBoundsException
    // in the event of a syntax error.  We expect that to be caught in
    // eval below.

    private boolean isOperatorUnchecked(int i, int op) {
        Token t = mExpr.get(i);
        if (!(t instanceof Operator)) return false;
        return ((Operator)(t)).mId == op;
    }

    private boolean isOperator(int i, int op, EvalContext ec) {
        if (i >= ec.mPrefixLength) return false;
        return isOperatorUnchecked(i, op);
    }

    static class SyntaxException extends Exception {
        public SyntaxException() {
            super();
        }
        public SyntaxException(String s) {
            super(s);
        }
    }

    // The following functions all evaluate some kind of expression
    // starting at position i in mExpr in a specified evaluation context.
    // They return both the expression value (as constructive real and,
    // if applicable, as BigInteger) and the position of the next token
    // that was not used as part of the evaluation.
    private EvalRet evalUnary(int i, EvalContext ec) throws SyntaxException {
        Token t = mExpr.get(i);
        BoundedRational ratVal;
        CR value;
        if (t instanceof Constant) {
            Constant c = (Constant)t;
            ratVal = c.toRational();
            value = ratVal.CRValue();
            return new EvalRet(i+1, value, ratVal);
        }
        if (t instanceof PreEval) {
            PreEval p = (PreEval)t;
            return new EvalRet(i+1, p.mValue, p.mRatValue);
        }
        EvalRet argVal;
        switch(((Operator)(t)).mId) {
        case R.id.const_pi:
            return new EvalRet(i+1, CR.PI, null);
        case R.id.const_e:
            return new EvalRet(i+1, REAL_E, null);
        case R.id.op_sqrt:
            // Seems to have highest precedence.
            // Does not add implicit paren.
            // Does seem to accept a leading minus.
            if (isOperator(i+1, R.id.op_sub, ec)) {
                argVal = evalUnary(i+2, ec);
                ratVal = BoundedRational.sqrt(
                                BoundedRational.negate(argVal.mRatVal));
                if (ratVal != null) break;
                return new EvalRet(argVal.mPos,
                                   argVal.mVal.negate().sqrt(), null);
            } else {
                argVal = evalUnary(i+1, ec);
                ratVal = BoundedRational.sqrt(argVal.mRatVal);
                if (ratVal != null) break;
                return new EvalRet(argVal.mPos, argVal.mVal.sqrt(), null);
            }
        case R.id.lparen:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            return new EvalRet(argVal.mPos, argVal.mVal, argVal.mRatVal);
        case R.id.fun_sin:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = ec.mDegreeMode ? BoundedRational.degreeSin(argVal.mRatVal)
                                     : BoundedRational.sin(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos,
                    toRadians(argVal.mVal,ec).sin(), null);
        case R.id.fun_cos:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = ec.mDegreeMode ? BoundedRational.degreeCos(argVal.mRatVal)
                                     : BoundedRational.cos(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos,
                    toRadians(argVal.mVal,ec).cos(), null);
        case R.id.fun_tan:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = ec.mDegreeMode ? BoundedRational.degreeTan(argVal.mRatVal)
                                     : BoundedRational.tan(argVal.mRatVal);
            if (ratVal != null) break;
            CR argCR = toRadians(argVal.mVal, ec);
            return new EvalRet(argVal.mPos,
                    argCR.sin().divide(argCR.cos()), null);
        case R.id.fun_ln:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = BoundedRational.ln(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos, argVal.mVal.ln(), null);
        case R.id.fun_exp:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = BoundedRational.exp(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos, argVal.mVal.exp(), null);
        case R.id.fun_log:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = BoundedRational.log(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos,
                               argVal.mVal.ln().divide(CR.valueOf(10).ln()),
                               null);
        case R.id.fun_arcsin:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = ec.mDegreeMode ? BoundedRational.degreeAsin(argVal.mRatVal)
                                     : BoundedRational.asin(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos,
                               fromRadians(UnaryCRFunction
                                   .asinFunction.execute(argVal.mVal),ec),
                               null);
        case R.id.fun_arccos:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = ec.mDegreeMode ? BoundedRational.degreeAcos(argVal.mRatVal)
                                     : BoundedRational.acos(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos,
                               fromRadians(UnaryCRFunction
                                   .acosFunction.execute(argVal.mVal),ec),
                               null);
        case R.id.fun_arctan:
            argVal = evalExpr(i+1, ec);
            if (isOperator(argVal.mPos, R.id.rparen, ec)) argVal.mPos++;
            ratVal = ec.mDegreeMode ? BoundedRational.degreeAtan(argVal.mRatVal)
                                     : BoundedRational.atan(argVal.mRatVal);
            if (ratVal != null) break;
            return new EvalRet(argVal.mPos,
                               fromRadians(UnaryCRFunction
                                   .atanFunction.execute(argVal.mVal),ec),
                               null);
        default:
            throw new SyntaxException("Unrecognized token in expression");
        }
        // We have a rational value.
        return new EvalRet(argVal.mPos, ratVal.CRValue(), ratVal);
    }

    // Compute an integral power of a constructive real.
    // Unlike the "general" case using logarithms, this handles a negative
    // base.
    private static CR pow(CR base, BigInteger exp) {
        if (exp.compareTo(BigInteger.ZERO) < 0) {
            return pow(base, exp.negate()).inverse();
        }
        if (exp.equals(BigInteger.ONE)) return base;
        if (exp.and(BigInteger.ONE).intValue() == 1) {
            return pow(base, exp.subtract(BigInteger.ONE)).multiply(base);
        }
        if (exp.equals(BigInteger.ZERO)) {
            return CR.valueOf(1);
        }
        CR tmp = pow(base, exp.shiftRight(1));
        return tmp.multiply(tmp);
    }

    private static final int TEST_PREC = -100;
                     // Test for integer-ness to 100 bits past binary point.
    private static final BigInteger MASK =
            BigInteger.ONE.shiftLeft(-TEST_PREC).subtract(BigInteger.ONE);
    private static final CR REAL_E = CR.valueOf(1).exp();
    private static final CR REAL_ONE_HUNDREDTH = CR.valueOf(100).inverse();
    private static final BoundedRational RATIONAL_ONE_HUNDREDTH =
            new BoundedRational(1,100);
    private static boolean isApprInt(CR x) {
        BigInteger appr = x.get_appr(TEST_PREC);
        return appr.and(MASK).signum() == 0;
    }

    private EvalRet evalSuffix(int i, EvalContext ec) throws SyntaxException {
        EvalRet tmp = evalUnary(i, ec);
        int cpos = tmp.mPos;
        CR cval = tmp.mVal;
        BoundedRational ratVal = tmp.mRatVal;
        boolean isFact;
        boolean isSquared = false;
        while ((isFact = isOperator(cpos, R.id.op_fact, ec)) ||
                (isSquared = isOperator(cpos, R.id.op_sqr, ec)) ||
                isOperator(cpos, R.id.op_pct, ec)) {
            if (isFact) {
                if (ratVal == null) {
                    // Assume it was an integer, but we
                    // didn't figure it out.
                    // KitKat may have used the Gamma function.
                    if (!isApprInt(cval)) {
                        throw new ArithmeticException("factorial(non-integer)");
                    }
                    ratVal = new BoundedRational(cval.BigIntegerValue());
                }
                ratVal = BoundedRational.fact(ratVal);
                cval = ratVal.CRValue();
            } else if (isSquared) {
                ratVal = BoundedRational.multiply(ratVal, ratVal);
                if (ratVal == null) {
                    cval = cval.multiply(cval);
                } else {
                    cval = ratVal.CRValue();
                }
            } else /* percent */ {
                ratVal = BoundedRational.multiply(ratVal, RATIONAL_ONE_HUNDREDTH);
                if (ratVal == null) {
                    cval = cval.multiply(REAL_ONE_HUNDREDTH);
                } else {
                    cval = ratVal.CRValue();
                }
            }
            ++cpos;
        }
        return new EvalRet(cpos, cval, ratVal);
    }

    private EvalRet evalFactor(int i, EvalContext ec) throws SyntaxException {
        final EvalRet result1 = evalSuffix(i, ec);
        int cpos = result1.mPos;  // current position
        CR cval = result1.mVal;   // value so far
        BoundedRational ratVal = result1.mRatVal;  // int value so far
        if (isOperator(cpos, R.id.op_pow, ec)) {
            final EvalRet exp = evalSignedFactor(cpos+1, ec);
            cpos = exp.mPos;
            // Try completely rational evaluation first.
            ratVal = BoundedRational.pow(ratVal, exp.mRatVal);
            if (ratVal != null) {
                return new EvalRet(cpos, ratVal.CRValue(), ratVal);
            }
            // Power with integer exponent is defined for negative base.
            // Thus we handle that case separately.
            // We punt if the exponent is an integer computed from irrational
            // values.  That wouldn't work reliably with floating point either.
            BigInteger int_exp = BoundedRational.asBigInteger(exp.mRatVal);
            if (int_exp != null) {
                cval = pow(cval, int_exp);
            } else {
                cval = cval.ln().multiply(exp.mVal).exp();
            }
            ratVal = null;
        }
        return new EvalRet(cpos, cval, ratVal);
    }

    private EvalRet evalSignedFactor(int i, EvalContext ec) throws SyntaxException {
        final boolean negative = isOperator(i, R.id.op_sub, ec);
        int cpos = negative ? i + 1 : i;
        EvalRet tmp = evalFactor(cpos, ec);
        cpos = tmp.mPos;
        CR cval = negative ? tmp.mVal.negate() : tmp.mVal;
        BoundedRational ratVal = negative ? BoundedRational.negate(tmp.mRatVal)
                                         : tmp.mRatVal;
        return new EvalRet(cpos, cval, ratVal);
    }

    private boolean canStartFactor(int i) {
        if (i >= mExpr.size()) return false;
        Token t = mExpr.get(i);
        if (!(t instanceof Operator)) return true;
        int id = ((Operator)(t)).mId;
        if (KeyMaps.isBinary(id)) return false;
        switch (id) {
            case R.id.op_fact:
            case R.id.rparen:
                return false;
            default:
                return true;
        }
    }

    private EvalRet evalTerm(int i, EvalContext ec) throws SyntaxException {
        EvalRet tmp = evalSignedFactor(i, ec);
        boolean is_mul = false;
        boolean is_div = false;
        int cpos = tmp.mPos;   // Current position in expression.
        CR cval = tmp.mVal;    // Current value.
        BoundedRational ratVal = tmp.mRatVal; // Current rational value.
        while ((is_mul = isOperator(cpos, R.id.op_mul, ec))
               || (is_div = isOperator(cpos, R.id.op_div, ec))
               || canStartFactor(cpos)) {
            if (is_mul || is_div) ++cpos;
            tmp = evalSignedFactor(cpos, ec);
            if (is_div) {
                ratVal = BoundedRational.divide(ratVal, tmp.mRatVal);
                if (ratVal == null) {
                    cval = cval.divide(tmp.mVal);
                } else {
                    cval = ratVal.CRValue();
                }
            } else {
                ratVal = BoundedRational.multiply(ratVal, tmp.mRatVal);
                if (ratVal == null) {
                    cval = cval.multiply(tmp.mVal);
                } else {
                    cval = ratVal.CRValue();
                }
            }
            cpos = tmp.mPos;
            is_mul = is_div = false;
        }
        return new EvalRet(cpos, cval, ratVal);
    }

    private EvalRet evalExpr(int i, EvalContext ec) throws SyntaxException {
        EvalRet tmp = evalTerm(i, ec);
        boolean is_plus;
        int cpos = tmp.mPos;
        CR cval = tmp.mVal;
        BoundedRational ratVal = tmp.mRatVal;
        while ((is_plus = isOperator(cpos, R.id.op_add, ec))
               || isOperator(cpos, R.id.op_sub, ec)) {
            tmp = evalTerm(cpos+1, ec);
            if (is_plus) {
                ratVal = BoundedRational.add(ratVal, tmp.mRatVal);
                if (ratVal == null) {
                    cval = cval.add(tmp.mVal);
                } else {
                    cval = ratVal.CRValue();
                }
            } else {
                ratVal = BoundedRational.subtract(ratVal, tmp.mRatVal);
                if (ratVal == null) {
                    cval = cval.subtract(tmp.mVal);
                } else {
                    cval = ratVal.CRValue();
                }
            }
            cpos = tmp.mPos;
        }
        return new EvalRet(cpos, cval, ratVal);
    }

    // Externally visible evaluation result.
    public class EvalResult {
        EvalResult (CR val, BoundedRational ratVal) {
            mVal = val;
            mRatVal = ratVal;
        }
        final CR mVal;
        final BoundedRational mRatVal;
    }

    /**
     * Return the starting position of the sequence of trailing binary operators.
     */
    private int trailingBinaryOpsStart() {
        int result = mExpr.size();
        while (result > 0) {
            Token last = mExpr.get(result - 1);
            if (!(last instanceof Operator)) break;
            Operator o = (Operator)last;
            if (!KeyMaps.isBinary(o.mId)) break;
            --result;
        }
        return result;
    }

    // Is the current expression worth evaluating?
    public boolean hasInterestingOps() {
        int last = trailingBinaryOpsStart();
        int first = 0;
        if (last > first && isOperatorUnchecked(first, R.id.op_sub)) {
            // Leading minus is not by itself interesting.
            first++;
        }
        for (int i = first; i < last; ++i) {
            Token t1 = mExpr.get(i);
            if (t1 instanceof Operator
                    || t1 instanceof PreEval && ((PreEval)t1).hasEllipsis()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluate the expression excluding trailing binary operators.
     * Errors result in exceptions, most of which are unchecked.
     * Should not be called concurrently with modification of the expression.
     * May take a very long time; avoid calling from UI thread.
     *
     * @param degreeMode use degrees rather than radians
     */
    EvalResult eval(boolean degreeMode) throws SyntaxException
                        // And unchecked exceptions thrown by CR
                        // and BoundedRational.
    {
        try {
            // We currently never include trailing binary operators, but include
            // other trailing operators.
            // Thus we usually, but not always, display results for prefixes
            // of valid expressions, and don't generate an error where we previously
            // displayed an instant result.  This reflects the Android L design.
            int prefixLen = trailingBinaryOpsStart();
            EvalContext ec = new EvalContext(degreeMode, prefixLen);
            EvalRet res = evalExpr(0, ec);
            if (res.mPos != prefixLen) {
                throw new SyntaxException("Failed to parse full expression");
            }
            return new EvalResult(res.mVal, res.mRatVal);
        } catch (IndexOutOfBoundsException e) {
            throw new SyntaxException("Unexpected expression end");
        }
    }

    // Produce a string representation of the expression itself
    SpannableStringBuilder toSpannableStringBuilder(Context context) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (Token t: mExpr) {
            ssb.append(t.toCharSequence(context));
        }
        return ssb;
    }
}
