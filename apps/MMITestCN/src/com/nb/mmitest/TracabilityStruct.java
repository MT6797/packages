package com.nb.mmitest;

//import java.util.Map;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.os.ServiceManager;

//import com.android.qualcomm.qcnvitems.QcNvItemTypes;
//import com.android.qualcomm.qcnvitems.QcNvItemsWXKJ;
//import com.android.qualcomm.qcnvitems.QcNvItems;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;

class TracabilityStruct {

	static int[] TracabilityInfoItems = { QcNvItemsWXKJ.NV_TRACABILITY_I,
			QcNvItemsWXKJ.NV_TRACABILITY_1_I, QcNvItemsWXKJ.NV_TRACABILITY_2_I,
			QcNvItemsWXKJ.NV_TRACABILITY_3_I };

	private static FactoryInfo mTracabilityData = new FactoryInfo(512,
			QcNvItemsWXKJ.NV_TRACABILITY_I /* TracabilityInfoItems */);

	private static FactoryInfo mMmitestInfo = new FactoryInfo(4,
			QcNvItemsWXKJ.NV_MMITEST_INFO_I);

	private static String TAG = "TracabilityStruct";

	private static int nbinstance = 0;

	/*
	 * The main components of this class are declared static so their value is
	 * shared between all users One client sync cache with NV when writing
	 * values
	 */

	TracabilityStruct() throws Exception {
		// check the tables are valid
		for (int i = 0; i < ID.TRACABILITY_ITEM_TYPE_MAX.ordinal(); i++) {
			if (map[i].id.ordinal() != i) {
				throw new Exception("Invalid tracability map");
			}
		}

		if (mTracabilityData == null)
			mTracabilityData = new FactoryInfo(512,
					QcNvItemsWXKJ.NV_TRACABILITY_I /* TracabilityInfoItems */);

		if (mMmitestInfo == null)
			mMmitestInfo = new FactoryInfo(4, QcNvItemsWXKJ.NV_MMITEST_INFO_I);

	}

	public static enum ID {
		REF_PCBA_I, SHORT_CODE_I, ICS_I, SITE_FAC_PCBA_I, LINE_FAC_PCBA_I, DATE_PROD_PCBA_I, SN_PCBA_I, INDUS_REF_HANDSET_I, INFO_PTM_I, SITE_FAC_HANDSET_I, LINE_FAC_HANDSET_I, DATE_PROD_HANDSET_I, SN_HANDSET_I, INFO_PTS_MINI_I, INFO_NAME_MINI_I, INFO_TECH_MINI_I, INFO_GOLDEN_FLAG_I, INFO_GOLDEN_DATE_I, INFO_ID_BAIE_HDTB_I, INFO_DATE_PASS_HDTB_I, INFO_PROD_BAIE_PARA_SYS_I, INFO_STATUS_PARA_SYS_I, INFO_NBRE_PASS_PARA_SYS_I, INFO_DATE_PASS_PARA_SYS_I, INFO_PROD_BAIE_PARA_SYS_2_I, INFO_STATUS_PARA_SYS_2_I, INFO_NBRE_PASS_PARA_SYS_2_I, INFO_DATE_PASS_PARA_SYS_2_I, INFO_PROD_BAIE_PARA_SYS_3_I, INFO_STATUS_PARA_SYS_3_I, INFO_NBRE_PASS_PARA_SYS_3_I, INFO_DATE_PASS_PARA_SYS_3_I, INFO_PROD_BAIE_BW_I, INFO_STATUS_BW_I, INFO_NBRE_PASS_BW_I, INFO_DATE_BAIE_BW_I, INFO_PROD_BAIE_GPS_I, INFO_STATUS_GPS_I, INFO_NBRE_PASS_GPS_I, INFO_DATE_BAIE_GPS_I, INFO_STATUS_MMI_TEST_I, INFO_PROD_BAIE_FINAL_I, INFO_STATUS_FINAL_I, INFO_NBRE_PASS_FINAL_I, INFO_DATE_BAIE_FINAL_I, INFO_PROD_BAIE_FINAL_2_I, INFO_STATUS_FINAL_2_I, INFO_NBRE_PASS_FINAL_2_I, INFO_DATE_BAIE_FINAL_2_I, INFO_ID_BAIE_HDT_I, INFO_DATE_PASS_HDT_I, INFO_COMM_REF_I, INFO_PTS_APPLI_I, INFO_NAME_APPLI_I, INFO_NAME_PERSO1_I, INFO_NAME_PERSO2_I, INFO_NAME_PERSO3_I, INFO_NAME_PERSO4_I, INFO_SPARE_REGION_I, BT_ADDR_I, WIFI_ADDR_I, TRACABILITY_ITEM_TYPE_MAX,
	};

	static class map_item_type {
		ID id;
		int size;
		String name;

		map_item_type(ID i, int s) {
			id = i;
			size = s;
		}
	}

	static map_item_type[] map = {
	/* PCBA Info */
	new map_item_type(ID.REF_PCBA_I, 12),
			new map_item_type(ID.SHORT_CODE_I, 4),
			new map_item_type(ID.ICS_I, 2),
			new map_item_type(ID.SITE_FAC_PCBA_I, 1),
			new map_item_type(ID.LINE_FAC_PCBA_I, 1),
			new map_item_type(ID.DATE_PROD_PCBA_I, 3),
			new map_item_type(ID.SN_PCBA_I, 4),
			/* Handset Info */
			new map_item_type(ID.INDUS_REF_HANDSET_I, 12),
			new map_item_type(ID.INFO_PTM_I, 2),
			new map_item_type(ID.SITE_FAC_HANDSET_I, 1),
			new map_item_type(ID.LINE_FAC_HANDSET_I, 1),
			new map_item_type(ID.DATE_PROD_HANDSET_I, 3),
			new map_item_type(ID.SN_HANDSET_I, 4),
			/* Mini Info */
			new map_item_type(ID.INFO_PTS_MINI_I, 3),
			new map_item_type(ID.INFO_NAME_MINI_I, 20),
			new map_item_type(ID.INFO_TECH_MINI_I, 20),
			/* Golden Sample */
			new map_item_type(ID.INFO_GOLDEN_FLAG_I, 1),
			new map_item_type(ID.INFO_GOLDEN_DATE_I, 3),
			/* HDTB (Rework) */
			new map_item_type(ID.INFO_ID_BAIE_HDTB_I, 3),
			new map_item_type(ID.INFO_DATE_PASS_HDTB_I, 3),
			/* PT1 Test */
			new map_item_type(ID.INFO_PROD_BAIE_PARA_SYS_I, 3),
			new map_item_type(ID.INFO_STATUS_PARA_SYS_I, 1),
			new map_item_type(ID.INFO_NBRE_PASS_PARA_SYS_I, 1),
			new map_item_type(ID.INFO_DATE_PASS_PARA_SYS_I, 3),
			/* PT2 Test */
			new map_item_type(ID.INFO_PROD_BAIE_PARA_SYS_2_I, 3),
			new map_item_type(ID.INFO_STATUS_PARA_SYS_2_I, 1),
			new map_item_type(ID.INFO_NBRE_PASS_PARA_SYS_2_I, 1),
			new map_item_type(ID.INFO_DATE_PASS_PARA_SYS_2_I, 3),
			/* PT3 Test */
			new map_item_type(ID.INFO_PROD_BAIE_PARA_SYS_3_I, 3),
			new map_item_type(ID.INFO_STATUS_PARA_SYS_3_I, 1),
			new map_item_type(ID.INFO_NBRE_PASS_PARA_SYS_3_I, 1),
			new map_item_type(ID.INFO_DATE_PASS_PARA_SYS_3_I, 3),
			/* Bluetooth/WI-FI Test */
			new map_item_type(ID.INFO_PROD_BAIE_BW_I, 3),
			new map_item_type(ID.INFO_STATUS_BW_I, 1),
			new map_item_type(ID.INFO_NBRE_PASS_BW_I, 1),
			new map_item_type(ID.INFO_DATE_BAIE_BW_I, 3),
			/* GPS Test */
			new map_item_type(ID.INFO_PROD_BAIE_GPS_I, 3),
			new map_item_type(ID.INFO_STATUS_GPS_I, 1),
			new map_item_type(ID.INFO_NBRE_PASS_GPS_I, 1),
			new map_item_type(ID.INFO_DATE_BAIE_GPS_I, 3),
			/* MMI Test */
			new map_item_type(ID.INFO_STATUS_MMI_TEST_I, 1),
			/* Final Test (2G Antenna Test) */
			new map_item_type(ID.INFO_PROD_BAIE_FINAL_I, 3),
			new map_item_type(ID.INFO_STATUS_FINAL_I, 1),
			new map_item_type(ID.INFO_NBRE_PASS_FINAL_I, 1),
			new map_item_type(ID.INFO_DATE_BAIE_FINAL_I, 3),
			/* Final Test 2 (3G Antenna Test) */
			new map_item_type(ID.INFO_PROD_BAIE_FINAL_2_I, 3),
			new map_item_type(ID.INFO_STATUS_FINAL_2_I, 1),
			new map_item_type(ID.INFO_NBRE_PASS_FINAL_2_I, 1),
			new map_item_type(ID.INFO_DATE_BAIE_FINAL_2_I, 3),
			/* HDT (CU perso downloading) , */
			new map_item_type(ID.INFO_ID_BAIE_HDT_I, 3),
			new map_item_type(ID.INFO_DATE_PASS_HDT_I, 3),
			/* CU SW info , */
			new map_item_type(ID.INFO_COMM_REF_I, 20),
			new map_item_type(ID.INFO_PTS_APPLI_I, 3),
			new map_item_type(ID.INFO_NAME_APPLI_I, 20),
			new map_item_type(ID.INFO_NAME_PERSO1_I, 20),
			new map_item_type(ID.INFO_NAME_PERSO2_I, 20),
			new map_item_type(ID.INFO_NAME_PERSO3_I, 20),
			new map_item_type(ID.INFO_NAME_PERSO4_I, 20),

			new map_item_type(ID.INFO_SPARE_REGION_I, 20),
			new map_item_type(ID.BT_ADDR_I, 6),
			new map_item_type(ID.WIFI_ADDR_I, 6),

	// new map_item_type(ID.INFO_SPARE_REGION_I , 223)
	};

	/*
	 * getItem
	 * 
	 * reads an item from a tracability table returns the item datas
	 */

	public byte[] getItem(ID id) {
		byte[] result = new byte[map[id.ordinal()].size];
		int offset = 0;

		// mTracabilityData.read();

		IBinder binder = ServiceManager.getService("NvRAMAgent");
		NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
		byte[] buff = null;
		try {
                        
			buff = agent.readFile((int) 35);
	                

			//buff=WXKJRapi.read_trace_parti();
			
			Log.v("MTK71025", "buff.length= " + buff.length);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG,"read raw data fail!");
		}

		// get the offset of the desired item
		for (int i = 0; i < map.length; i++) {
			if (map[i].id.equals(id)) {
				break;
			} else {
				offset += map[i].size;
			}
		}
		// extract the item datas from the tracability buffer
		try {
			System.arraycopy(buff, offset, result, 0, result.length);
		} catch (Exception e) {
			Log.d(TAG, "Can't read Tracability item " + id.toString());
		}
		return result;
	}

	/*
	 * putItem
	 * 
	 * puts an item from a tracability table
	 */

	public void putItem(ID id, byte[] data) {
		int offset = 0;
		byte[] buff = null;

		/* read PRODUCT_INFO file from nvram section */
		IBinder binder = ServiceManager.getService("NvRAMAgent");
		NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
		try {
                      
			buff = agent.readFile((int) 35);
                        

		} catch (RemoteException e) {
			e.printStackTrace();
		}

		/* get the offset of the desired item */
		for (int i = 0; i < map.length; ++i) {
			if (map[i].id.equals(id)) {
				break;
			} else {
				offset += map[i].size;
			}
		}

		if (data.length != map[id.ordinal()].size) {
			throw new InvalidParameterException("Item size is wrong");
		}

		for (int i = 0; i < data.length; ++i) {
			Arrays.fill(buff, offset + i, offset + i + 1, data[i]);
		}

		if (true) {
			try {
                               
				agent.writeFile((int) 35, buff);
                               
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public int getMmitestInfo() {
		mMmitestInfo.read();

		int result = ((int) mMmitestInfo.data[0] << 0) & 0x000000FF
				| ((int) mMmitestInfo.data[1] << 8) & 0x0000FF00
				| ((int) mMmitestInfo.data[2] << 16) & 0x00FF0000
				| ((int) mMmitestInfo.data[3] << 24) & 0xFF000000;

		return result;
	}

	public void setMmitestInfo(int mStatusBits) {

		byte[] data = new byte[4];

		data[0] = (byte) (mStatusBits >> 0);
		data[1] = (byte) (mStatusBits >> 8);
		data[2] = (byte) (mStatusBits >> 16);
		data[3] = (byte) (mStatusBits >> 24);

		mMmitestInfo.write(data, 0);
	}

	@Override
	public String toString() {
		String result = "";
		for (int i = 0; i < map.length; i++) {
			result += map[i].id.toString();
			result += "\n";

		}

		return result;
	}

}
