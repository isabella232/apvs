package ch.cern.atlas.apvs.hibernate.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

@SuppressWarnings("rawtypes")
public class ListDoubleTypeDescriptor extends AbstractTypeDescriptor<List> {

	private static final long serialVersionUID = -1287517556655783456L;
	public static final ListDoubleTypeDescriptor INSTANCE = new ListDoubleTypeDescriptor();

	public ListDoubleTypeDescriptor() {
		super( List.class );
	}
	
	public String toString(List value) {
		StringBuffer b = new StringBuffer();
		for (@SuppressWarnings("unchecked") Iterator<Double> i = (Iterator<Double>)value.iterator(); i.hasNext(); ) {
			Double d = i.next();
			b.append(d);
			if (i.hasNext()) {
				b.append(", ");
			}
		}
		return b.toString();
	}

	public List<Double> fromString(String string) {
		String[] p = string.split(",");
		List<Double> r = new ArrayList<Double>(p.length);
		for (int i=0; i<p.length; i++) {
			r.add(Double.parseDouble(p[i].trim()));
		}
		return r;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(List value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( List.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		else if (String.class.isAssignableFrom( type ) ) {
			return (X) toString(value);
		}
		throw unknownUnwrap( type );
	}

	@SuppressWarnings("unchecked")
	public <X> List<Double> wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( List.class.isInstance( value ) ) {
			return (List<Double>) value;
		}
		else if ( String.class.isInstance( value ) ) {
			try {
				return fromString((String)value);
			} catch (IllegalArgumentException e) {
				throw unknownWrap( value.getClass() );
			}
		}
		throw unknownWrap( value.getClass() );
	}	

}
