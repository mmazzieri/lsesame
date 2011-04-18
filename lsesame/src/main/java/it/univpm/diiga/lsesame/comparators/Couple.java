package it.univpm.diiga.lsesame.comparators;

public class Couple {
	private String el1, el2;

	public Couple(String el1, String el2) {
		this.el1 = el1;
		this.el2 = el2;
	}

	public String getFirstElement() {
		return el1;
	}

	public String getSecondElement() {
		return el2;
	}

	public void setFirstElement(String el) {
		el1 = el;
	}

	public void setSecondElement(String el) {
		el2 = el;
	}
}
