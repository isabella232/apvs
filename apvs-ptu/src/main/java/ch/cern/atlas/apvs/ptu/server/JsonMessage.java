package ch.cern.atlas.apvs.ptu.server;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.domain.Device;
import ch.cern.atlas.apvs.domain.Error;
import ch.cern.atlas.apvs.domain.Event;
import ch.cern.atlas.apvs.domain.GeneralConfiguration;
import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.domain.MeasurementConfiguration;
import ch.cern.atlas.apvs.domain.Message;
import ch.cern.atlas.apvs.domain.Order;
import ch.cern.atlas.apvs.domain.Report;
import ch.cern.atlas.apvs.domain.SensorOrder;

import com.cedarsoftware.util.io.JsonObject;

public class JsonMessage {

	private static Logger log = LoggerFactory.getLogger(JsonMessage.class
			.getName());
	
	private static String TYPE = "Type";
	private static String SENSOR = "Sensor";
	private static String UNIT = "Unit";
	private static String VALUE = "Value";
	private static String TIME = "Time";
	private static String SAMPLING_RATE = "SamplingRate";
	private static String UP_THRESHOLD = "UpThreshold";
	private static String DOWN_THRESHOLD = "DownThreshold";
	private static String METHOD = "Method";
	private static String THRESHOLD = "Threshold";
	private static String DESCRIPTION = "Description";
	private static String CRITICALITY = "Criticality";
	private static String DOSIMETER_ID = "DosimeterID";
	private static String BSSID = "bssid";
	private static String SLOPE = "Slope";
	private static String OFFSET = "Offset";
	private static String NAME = "Name";
	private static String PARAMETER = "Parameter";
	private static String BATTERY_LEVEL = "BatteryLevel";
	private static String CAMERA_HANDHELD = "CameraHandheld";
	private static String CAMERA_HELMET = "CameraHelmet";
	private static String AUDIO = "Audio";
	
	protected JsonObject<String, Object> msg;

	public JsonMessage(JsonObject<String, Object> msg) {
		this.msg = msg;
	}

	// FIXME add proper conversions (Date, Int, Double)
	public JsonMessage(Message message) {
		msg = new JsonObject<String, Object>();
		if (message instanceof Measurement) {
			Measurement m = (Measurement) message;
			msg.put(TYPE, m.getType());
			msg.put(SENSOR, m.getSensor());
			msg.put(UNIT, m.getUnit());
			if (m.getValueList() != null) {
				msg.put(VALUE, m.getValueList().toArray());
			} else {
				msg.put(VALUE, m.getValue().toString());
			}
			msg.put(TIME, m.getTime());
			msg.put(SAMPLING_RATE, m.getSamplingRate());
			msg.put(UP_THRESHOLD, m.getUpThreshold());
			msg.put(DOWN_THRESHOLD, m.getDownThreshold());
			msg.put(METHOD, m.getMethod());
		} else if (message instanceof Event) {
			Event m = (Event) message;
			msg.put(TYPE, m.getType());
			msg.put(VALUE, m.getValue());
			msg.put(THRESHOLD, m.getThreshold());
			msg.put(UNIT, m.getUnit());
			msg.put(TIME, m.getTime());
		} else if (message instanceof Error) {
			Error m = (Error) message;
			msg.put(TYPE, m.getType());
			msg.put(DESCRIPTION, m.getDescription());
			msg.put(CRITICALITY, m.getCriticality());
			msg.put(TIME, m.getTime());
		} else if (message instanceof GeneralConfiguration) {
			GeneralConfiguration m = (GeneralConfiguration) message;
			msg.put(TYPE, m.getType());
			msg.put(DOSIMETER_ID, m.getDosimeterId());
			msg.put(BSSID, m.getBSSID());
		} else if (message instanceof MeasurementConfiguration) {
			MeasurementConfiguration m = (MeasurementConfiguration) message;
			msg.put(TYPE, m.getType());
			msg.put(SENSOR, m.getSensor());
			msg.put(UNIT, m.getUnit());
			msg.put(TIME, m.getTime());
			msg.put(SAMPLING_RATE, m.getSamplingRate());
			msg.put(UP_THRESHOLD, m.getUpThreshold());
			msg.put(DOWN_THRESHOLD, m.getDownThreshold());
			msg.put(SLOPE, m.getSlope());
			msg.put(OFFSET, m.getOffset());
		} else if (message instanceof SensorOrder) {
			SensorOrder m = (SensorOrder) message;
			msg.put(TYPE, m.getType());
			msg.put(NAME, m.getName());
			msg.put(PARAMETER, m.getParameter());
			msg.put(VALUE, m.getValue());
		} else if (message instanceof Order) {
			Order m = (Order) message;
			msg.put(TYPE, m.getType());
			msg.put(PARAMETER, m.getParameter());
			msg.put(VALUE, m.getValue());
		} else if (message instanceof Report) {
			Report m = (Report) message;
			msg.put(TYPE, m.getType());
			msg.put(BATTERY_LEVEL, m.getBatteryLevel());
			msg.put(CAMERA_HANDHELD, m.getCameraHandheld());
			msg.put(CAMERA_HELMET, m.getCameraHelmet());
			msg.put(AUDIO, m.getAudio());
			msg.put(TIME, m.getTime());
		} else {
			throw new RuntimeException("Cannot find JsonMessage for '"
					+ message + "'");
		}
	}

	public Message toMessage(Device device) {
		String type = (String) msg.get(TYPE);
		if (type.equals("Measurement")) {
			String sensor = getString(SENSOR);
			String unit = getString(UNIT);			
			Double value = null;
			List<Double> valueList = null;
			if (msg.get(VALUE) instanceof JsonObject) {
				valueList = getDoubleList(VALUE);
			} else {
				value = getDouble(VALUE);
			}
			Date time = getDate(TIME);
			Integer samplingRate = getInteger(SAMPLING_RATE);

			// fix for #486 and #490
			if ((sensor == null) || ((value == null) && (valueList == null)) || (unit == null)
					|| (time == null)) {
				log.warn("PTU "
						+ device.getName()
						+ ": Measurement contains <null> sensor, value, samplingrate, unit or time ("
						+ sensor + ", " + value + ", " + unit + ", "
						+ samplingRate + ", " + time + ")");
				return null;
			}

			if (valueList != null) {
				return new Measurement(device, sensor, valueList,
						getDouble(DOWN_THRESHOLD), getDouble(UP_THRESHOLD), unit,
						samplingRate, getString(METHOD), time);
			} 
			return new Measurement(device, sensor, value,
					getDouble(DOWN_THRESHOLD), getDouble(UP_THRESHOLD), unit,
					samplingRate, getString(METHOD), time);
		} else if (type.equals("Event")) {
			return new Event(device, getString(SENSOR),
					getString("EventType"), getDouble(VALUE),
					getDouble(THRESHOLD), getString(UNIT), getDate(TIME));
		} else if (type.equals("Error")) {
			return new Error(device, getInteger("ErrNo"),
					getString(DESCRIPTION), getString(CRITICALITY),
					getDate(TIME));
		} else if (type.equals("GeneralConfiguration")) {
			return new GeneralConfiguration(device, getString(DOSIMETER_ID), getString(BSSID), getDate(TIME));
		} else if (type.equals("MeasurementConfiguration")) {
			return new MeasurementConfiguration(device, getString(SENSOR), 
					getDouble(DOWN_THRESHOLD), getDouble(UP_THRESHOLD), getString(UNIT),
					getInteger(SAMPLING_RATE), getDouble(SLOPE), getDouble(OFFSET), getDate(TIME));
		} else if (type.equals("SensorOrder")) {
			return new SensorOrder(device, getString(NAME),
					getString(PARAMETER), getString(VALUE));
		} else if (type.equals("Order")) {
			return new Order(device, getString(PARAMETER), getString(VALUE));
		} else if (type.equals("Report")) {
			return new Report(device, getDouble(BATTERY_LEVEL),
					getBoolean(CAMERA_HANDHELD), getBoolean(CAMERA_HELMET),
					getBoolean(AUDIO), getDate(TIME));
		}

		log.warn("Ignoring message with unknown type: " + type + " " + msg);

		return null;
	}

	private String getString(String key) {
		return toString(msg.get(key));
	}

	private Double getDouble(String key) {
		return toDouble(msg.get(key));
	}
	
	private List<Double> getDoubleList(String key) {
		JsonObject o = (JsonObject)msg.get(key);
		Object[] a = o.getArray();
		List<Double> r = new ArrayList<Double>(a.length);
		for (int i=0; i<a.length; i++) {
			r.add((Double)a[i]);
		}
		return r;
	}

	private Integer getInteger(String key) {
		return toInteger(msg.get(key));
	}

	private Boolean getBoolean(String key) {
		return toBoolean(msg.get(key));
	}

	private Date getDate(String key) {
		return toDate(msg.get(key));
	}
	
	public static String toString(Object string) {
		if ((string == null) || !(string instanceof String)) {
			return null;
		}
		
		return string.toString();
	}

	public static Double toDouble(Object number) {
		System.out.println(number.getClass());
		if ((number == null) || !(number instanceof String)) {
			return null;
		}
		try {
			return Double.parseDouble((String) number);
		} catch (NumberFormatException e) {
			log.warn("NumberFormatException " + number + " " + e);
			return null;
		}
	}

	public static Integer toInteger(Object number) {
		if ((number == null) || !(number instanceof String)) {
			return null;
		}
		try {
			return Integer.parseInt((String) number);
		} catch (NumberFormatException e) {
			log.warn("NumberFormatException " + number + " " + e);
			return null;
		}
	}

	public static boolean toBoolean(Object state) {
		if ((state == null) || !(state instanceof String)) {
			return false;
		}
		try {
			String b = (String) state;
			b = b.equals("1") ? "true" : b.equals("0") ? "false" : b;
			return Boolean.parseBoolean((String) state);
		} catch (NumberFormatException e) {
			log.warn("NumberFormatException " + state + " " + e);
			return false;
		}
	}

	public static Date toDate(Object date) {
		try {
			return PtuServerConstants.dateFormat.parse((String) date);
		} catch (ParseException e) {
			return null;
		}
	}

	public String toString() {
		return msg.toString();
	}

	@SuppressWarnings("unused")
	private String limit(String value, int length) {
		StringBuilder buf = new StringBuilder(value);
		if (buf.length() > length) {
			buf.setLength(length);
		}

		return buf.toString();
	}
}
