package com.lti.ow.lambda.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.da.ItemState;

import com.lti.ow.lambda.property.Constants;

public class Formator {

	static final Logger log = Logger.getLogger(Formator.class);

	/*
	 * format timeseries date to given format
	 */
	public static String formatDate(Calendar calendar) {
		SimpleDateFormat fmt = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
		fmt.setCalendar(calendar);
		String dateFormatted = fmt.format(calendar.getTime());
		return dateFormatted;
	}

	/*
	 * parsing value coming from raw timeseries data
	 */
	public static String getParsedValue(ItemState itemState) throws JIException {

		switch (itemState.getValue().getType()) {

		case JIVariant.VT_VARIANT:
			return itemState.getValue().toString().substring(2, itemState.getValue().toString().length() - 2);

		case JIVariant.VT_BOOL:
			return String.valueOf(itemState.getValue().getObjectAsBoolean());

		case JIVariant.VT_DATE:
			return String.valueOf(itemState.getValue().getObjectAsDate());

		case JIVariant.VT_INT:
			return String.valueOf(itemState.getValue().getObjectAsInt());

		case JIVariant.VT_UINT:
			return String.valueOf(itemState.getValue().getObjectAsUnsigned().getValue());

		case JIVariant.VT_R4:
			return String.format("%.4f", itemState.getValue().getObjectAsFloat());

		case JIVariant.VT_R8:
			return String.format("%.4f", itemState.getValue().getObjectAsDouble());

		case JIVariant.VT_I2:
			return String.valueOf(itemState.getValue().getObjectAsShort());

		case JIVariant.VT_UI2:
			return String.valueOf(itemState.getValue().getObjectAsUnsigned().getValue());

		case JIVariant.VT_I1:
			return String.valueOf(itemState.getValue().getObjectAsChar());

		case JIVariant.VT_UI1:
			return String.valueOf(itemState.getValue().getObjectAsUnsigned().getValue());

		case JIVariant.VT_BSTR:
			return String.valueOf(itemState.getValue().getObjectAsString2());

		case JIVariant.VT_ARRAY:
			return String.valueOf(itemState.getValue().getObjectAsArray());

		case JIVariant.VT_I8:
			return String.valueOf(itemState.getValue().getObjectAsLong());

		default:
			return null;

		}
	}

}
