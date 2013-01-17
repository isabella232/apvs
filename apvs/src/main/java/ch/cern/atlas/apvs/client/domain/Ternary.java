package ch.cern.atlas.apvs.client.domain;

/**
 * Using Kleene's Logic
 * 
 * http://en.wikipedia.org/wiki/Three-valued_logic
 * 
 * @author duns
 *
 */
public enum Ternary {
	True("True"), False("False"), Unknown("Unknown");

	private String s;

	private Ternary(String s) {
		this.s = s;
	}

	public String getString() {
		return s;
	}

	public Ternary not() {
		if (this.equals(True)) return False;
		if (this.equals(False)) return True;
		return Unknown;
	}

	public Ternary or(Ternary ternary) {
		if (this.equals(True) || ternary.equals(True)) return True;
		if (this.equals(False) && ternary.equals(False)) return False;
		return Unknown;
	}
	
	public Ternary and(Ternary ternary) {
		if (this.equals(True) && ternary.equals(True)) return True;
		if (this.equals(False) || ternary.equals(False)) return False;
		return Unknown;	
	}
	
	public boolean isTrue() {
		return this.equals(True);
	}
	
	public boolean isFalse() {
		return this.equals(False);
	}
	
	public boolean isUnknown() {
		return this.equals(Unknown);
	}
}
