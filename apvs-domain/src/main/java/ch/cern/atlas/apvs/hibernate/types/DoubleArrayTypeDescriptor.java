package ch.cern.atlas.apvs.hibernate.types;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

public class DoubleArrayTypeDescriptor extends AbstractTypeDescriptor<Double[]> {

	private static final long serialVersionUID = -1287517556655783456L;
	public static final DoubleArrayTypeDescriptor INSTANCE = new DoubleArrayTypeDescriptor();

	public DoubleArrayTypeDescriptor() {
		super( Double[].class );
	}
	
	public String toString(Double[] value) {
		StringBuffer b = new StringBuffer();
		for (int i=0; i<value.length; i++) {
			if (i>0) {
				b.append(", ");
			}
			b.append(value[i]);
		}
		return b.toString();
	}

	public Double[] fromString(String string) {
		String[] p = string.split(",");
		Double[] r = new Double[p.length];
		for (int i=0; i<p.length; i++) {
			try {
			    r[i] = Double.parseDouble(p[i].trim());
			} catch (NumberFormatException e) {
				r[i] = null;
			}
		}
		return r;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Double[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Double[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		else if (String.class.isAssignableFrom( type ) ) {
			return (X) toString(value);
		}
		throw unknownUnwrap( type );
	}

	public <X> Double[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Double[].class.isInstance( value ) ) {
			return (Double[]) value;
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
