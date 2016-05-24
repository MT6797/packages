package com.mediatek.contacts.ext;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.ContentProviderOperation.Builder;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountType.EditType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.ValuesDelta;

import java.util.ArrayList;

import android.accounts.Account;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;

public interface IAasExtension {
    // ----Flags for AAS
    public static int TYPE_FOR_PHONE_NUMBER = 0;
    public static final int TYPE_FOR_ADDITIONAL_NUMBER = 1;
    public static final char STRING_PRIMART = 0;
    public static final char STRING_ADDITINAL = 1;

    public static final int VIEW_UPDATE_NONE = 0;
    public static final int VIEW_UPDATE_HINT = 1;
    public static final int VIEW_UPDATE_VISIBILITY = 2;
    public static final int VIEW_UPDATE_DELETE_EDITOR = 3;
    public static final int VIEW_UPDATE_LABEL = 4;

    public static final int SIM_ID_DONT_KNOW_CURR = 10;

    /*
     * initially has been set by calling setCurrentSlot & at particular calling
     * of interface dont know the simid
     */

    /**
     * set the subId at plugin side
     *
     * @param subId
     *            the subId of editor.
     * @internal
     */
    public void setCurrentSubId(int subId);

    /**
     * This interface ensures phone kind is updated and exists.
     *
     * @param type
     *            the accountype of editor.
     * @param subId
     *            the subId of operation.
     * @param entity
     *            the RawContactDelta.
     * @return true if plugin has handled, false else,
     * @internal
     */
    public boolean ensurePhoneKindForEditor(AccountType type, int subId, RawContactDelta entity);

    /**
     * Reorders the ValuesDelta list to ensure primary number comes at first
     * position.
     *
     * @param state
     *            the RawContactDelta.
     * @param mimeType
     *            the mimetype of kind.
     * @return ArrayListofValuesDelta if mimeType is phone & plugin handled,
     *         call super else,
     * @internal
     */
    public ArrayList<ValuesDelta> rebuildFromState(RawContactDelta state, String mimeType);

    /**
     * Hides the spinner label for Email and Primary number.
     *
     * @param kind
     *            the DataKind.
     * @param entry
     *            the ValuesDelta.
     * @param state
     *            RawContactDelta,
     * @return true if plugin has hide the label of email and primary number,
     *         false else.
     * @internal
     */
    public boolean handleLabel(DataKind kind, ValuesDelta entry, RawContactDelta state);

    /**
     * Updates a view by action VIEW_UPDATE_HINT VIEW_UPDATE_DELETE_EDITOR
     * VIEW_UPDATE_VISIBILITY VIEW_UPDATE_LABEL
     *
     * @param state
     *            the RawContactDelta.
     * @param view
     *            the view to modify or update.
     * @param entry
     *            ValuesDelta,
     * @param action
     *            described above,
     * @return true if plugin has updated the viw based on action, false else.
     * @internal
     */
    public boolean updateView(RawContactDelta state, View view, ValuesDelta entry, int action);

    /**
     * Returns the max number of empty editors allowed on the editor screen.
     *
     * @param state
     *            the RawContactDelta.
     * @param mimeType
     *            the mime of kind
     * @return count maximum editor allowed for mimetype if plugin handles ,
     *         otherwise 1
     * @internal
     */
    public int getMaxEmptyEditors(RawContactDelta state, String mimeType);

    /**
     * Returns customized AAS tags by id present in customcolumn.
     *
     * @param type
     *            the DATA2 or type of contact.
     * @param customColumn
     *            the aas index
     * @return String of aastag , none.if plugin handles, else null
     * @internal
     */
    public String getCustomTypeLabel(int type, String customColumn);

    /**
     * Sets the current selection on the spinner to the EditType corresponding
     * to the current AAS tag ID
     *
     * @param state
     *            the RawContactDelta.
     * @param label
     *            the spinner for label selection
     * @param adapter
     *            the adapter of spinner label
     * @param item
     *            the selected item
     * @param kind
     *            the datakind
     * @return true if plugin has handled it successfully , false else
     * @internal
     */
    public boolean rebuildLabelSelection(RawContactDelta state, Spinner label,
            ArrayAdapter<EditType> adapter, EditType item, DataKind kind);

    /**
     * Handles the spinner item selection by the user
     *
     * @param rawContact
     *            the RawContactDelta.
     * @param entry
     *            the ValuesDelta
     * @param kind
     *            the DataKind
     * @param editTypeAdapter
     *            the adapter of labels
     * @param select
     *            the selected item in spinner
     * @param type
     *            the last saved spinner item
     * @return true if plugin has handled it successfully , false else
     * @internal
     */
    public boolean onTypeSelectionChange(RawContactDelta rawContact, ValuesDelta entry,
            DataKind kind, ArrayAdapter<EditType> editTypeAdapter, EditType select, EditType type);

    /**
     * Returns the EditType in the DataKind typelist, which matches the AAS tag
     * ID in the ValuesDelta entry.
     *
     * @param entry
     *            the ValuesDelta.
     * @param kind
     *            the DataKind
     * @param rawValue
     *            the type 2, 7, 101
     * @return EditType if plugin handles return the editType for this rawvalue
     *         and kind, mime
     * @internal
     */
    public EditType getCurrentType(ValuesDelta entry, DataKind kind, int rawValue);

    /**
     * While loading the USIM contacts parts to create masterDB check AAS column
     * in cursor & update the builder for DATA2& DATA3 if accountType is USIM
     * then only
     *
     * @param accountType
     *            the Accounytype for processing
     * @param builder
     *            The ContentProviderOperation will use to write DB
     * @param cursor
     *            the cursor have contact info
     * @param type
     *            the type have the info for primary number or additional number
     * @internal
     */
    public void updateOperation(String accountType, ContentProviderOperation.Builder builder,
            Cursor cursor, int type);

    /**
     * While copying phone contacts to USIM, get the contacts additional number
     * by URI update the simContentValues for anr & aas for writing in USIMcard
     *
     * @param sourceUri
     *            the Uri for processing
     * @param subId
     *            The Contact associated with
     * @param accountType
     *            the type of account
     * @param simContentValues
     *            the string value pair to update for copy to insert in SIM in
     *            one go
     * @internal
     */
    public void updateValuesforCopy(Uri sourceUri, int subId, String accountType,
            ContentValues simContentValues);

    /**
     * update the column to dest builder
     *
     * @param srcCursor
     *            the source cursor
     * @param destBuilder
     *            the builde update to
     * @param srcAccountType
     *            the account type of source
     * @param srcMimeType
     *            the mime type of source data
     * @param destSubId
     *            the destination sub id of contact
     * @param indexOfColumn
     *            the index of column in source cursor.
     * @return true if update the destination builder,false else.
     * @internal
     */
    public boolean cursorColumnToBuilder(Cursor srcCursor, Builder destBuilder,
            String srcAccountType, String srcMimeType, int destSubId, int indexOfColumn);

    /**
     * check a contentvalue is for additional number or not
     *
     * @param cv
     *            the cv for check & processing
     * @return true if cv have the additional number false else.
     * @internal
     */
    public boolean checkAasEntry(ContentValues cv);

    /**
     * update the contentValues before writing to USIMCard for anr/newanr or aas
     *
     * @param intent
     *            the editor screen data & contact info old and new both
     * @param sbuId
     *            which sbuId is under operation
     * @param contentValues
     *            to process & update for anr & aas
     * @return true if updated the contentValues ,false else.
     * @internal
     */
    public boolean updateValues(Intent intent, int sbuId, ContentValues contentValues);

    /**
     * Edit a contact and saving it so for updating the additional number info
     * in masterDB
     *
     * @param intent
     *            the editor screen data & contact info old and new both
     * @param rawContactId
     *            which row has to update by making where clause
     * @return true if updated the additional number to DB ,false else.
     * @internal
     */
    public boolean updateAdditionalNumberToDB(Intent intent, long rawContactId);

    /**
     * when a new contact is going to insert in masterdatabase after creating
     * entry in USIM Add anr info to operationlist so that in one batch it all
     * process together
     *
     * @param accounType
     *            the account SIM/USIM
     * @param operationList
     *            add the aas and anr info to operationlist
     * @param backRef
     *            back references
     * @return true if the plugin update the operation list , false else.
     * @internal
     */
    public boolean updateOperationList(Account accounType,
            ArrayList<ContentProviderOperation> operationList, int backRef);

    /* this will be used for saving a new contact & in copy operation */

    /**
     *
     * @param res
     *            the host res context
     * @param type
     *            data2 of contact
     * @param customLabel
     *            data3 of contact
     * @param mimeType
     *            mimetype of kind
     * @param cursor
     *            cursor have info of contact
     * @param defaultValue
     *            label already expected to show
     * @return CharSequence if the plugin have to update the label ,
     *         defaultValue else.
     * @internal
     */
    public CharSequence getLabelForBindData(Resources res, int type, String customLabel,
            String mimeType, Cursor cursor, CharSequence defaultValue);

    /**
     * get the label of phonenumber primary or additional
     *
     * @param type
     *            data2 of contact
     * @param label
     *            data3 of contact
     * @param defaultValue
     *            label already expected to show
     * @param subId
     *            asking typelabel contact is associated to which sub id
     * @return CharSequence if the plugin have to update the label ,
     *         defaultValue else.
     * @internal
     */
    public CharSequence getTypeLabel(int type, CharSequence label, String defvalue, int subId);

    /**
     * display a contact details for mark the number as primary or additional
     * number
     *
     * @param subId
     *            the subId of contact associated with phone or SIM
     * @param type
     *            type data2 of contact table
     * @return null if not to show ,else actual string as subHeader.
     * @internal
     */
    public String getSubheaderString(int subId, int type);

    /**
     * This interface ensures phone kind is updated and exists.
     *
     * @param state
     *            the RawContactDeltaList of editor.
     * @param subId
     *            the subId of operation.
     * @param context
     *            the ApplicationContext.
     * @internal
     */
    public void ensurePhoneKindForCompactEditor(RawContactDeltaList state,
            int subId, Context context);

    /**
     * This interface check current edit contact is from SIM.
     *
     * @param isSimContact
     *            SIM Contact flag
     */
    public boolean shouldStopSave(boolean isSimContact);
}
