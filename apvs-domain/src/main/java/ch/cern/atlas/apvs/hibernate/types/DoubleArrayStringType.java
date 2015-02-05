package ch.cern.atlas.apvs.hibernate.types;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Store a array of doubles as a string in the DB
 * 
 * @author duns
 * 
 */
public class DoubleArrayStringType extends
		AbstractSingleColumnStandardBasicType<Double[]> implements
		DiscriminatorType<Double[]> {
	private static final long serialVersionUID = 3811773286272786399L;
	public static final DoubleArrayStringType INSTANCE = new DoubleArrayStringType();

	public DoubleArrayStringType() {
		super(VarcharTypeDescriptor.INSTANCE, DoubleArrayTypeDescriptor.INSTANCE);
	}

	public String getName() {
		return "double_array_string";
	}

	@Override
	public String toString(Double[] value) {
		return DoubleArrayTypeDescriptor.INSTANCE.toString(value);
	}

	@Override
	public String objectToSQLString(Double[] value, Dialect dialect)
			throws Exception {
		return StringType.INSTANCE.objectToSQLString(toString(value), dialect);
	}

	@Override
	public Double[] stringToObject(String xml) throws Exception {
		return DoubleArrayTypeDescriptor.INSTANCE.fromString(xml);
	}
}
