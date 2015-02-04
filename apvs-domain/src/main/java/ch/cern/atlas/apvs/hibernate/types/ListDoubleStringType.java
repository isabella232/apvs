package ch.cern.atlas.apvs.hibernate.types;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Store a list of doubles as a string in the DB
 * 
 * @author duns
 * 
 */
@SuppressWarnings("rawtypes")
public class ListDoubleStringType extends
		AbstractSingleColumnStandardBasicType<List> implements
		DiscriminatorType<List<Double>> {
	private static final long serialVersionUID = 3811773286272786399L;
	public static final ListDoubleStringType INSTANCE = new ListDoubleStringType();

	public ListDoubleStringType() {
		super(VarcharTypeDescriptor.INSTANCE, ListDoubleTypeDescriptor.INSTANCE);
	}

	public String getName() {
		return "list_double_string";
	}

	@Override
	public String toString(List value) {
		return ListDoubleTypeDescriptor.INSTANCE.toString(value);
	}

	@Override
	public String objectToSQLString(List<Double> value, Dialect dialect)
			throws Exception {
		return StringType.INSTANCE.objectToSQLString(toString(value), dialect);
	}

	@Override
	public List<Double> stringToObject(String xml) throws Exception {
		return ListDoubleTypeDescriptor.INSTANCE.fromString(xml);
	}
}
