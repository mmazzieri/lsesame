package it.univpm.diiga.lsesame.comparators;

import it.univpm.diiga.lsesame.ResourceComparator;

import java.util.ArrayList;

import org.openrdf.model.Resource;

public class ContextComparerWithList implements ResourceComparator {
	private ArrayList<Couple> order;


	public ContextComparerWithList() {
		order = new ArrayList<Couple>();
		Couple cop;
		//Example with eight context precedences 
		for (int i = 0; i < 8; i++) {
			switch (i) {
			case 0:
				cop = new Couple("a", "b");
				break;
			case 1:
				cop = new Couple("b", "c");
				break;
			case 2:
				cop = new Couple("d", "c");
				break;
			case 3:
				cop = new Couple("c", "f");
				break;
			case 4:
				cop = new Couple("f", "e");
				break;
			case 5:
				cop = new Couple("g", "a");
				break;
			case 6:
				cop = new Couple("l", "d");
				break;
			case 7:
				cop = new Couple("g", "l");
				break;
			default:
				cop = null;
			}

			order.add(cop);

			calculateClosure();

		}
		

	}

	public void calculateClosure() {

		applyRuleRdfs5_1();
		applyRuleRdfs5_2();

	}

	public void applyRuleRdfs5_1() {

		for (int i = 0; i < order.size(); i++) {

			String aaa = order.get(i).getFirstElement();
			String bbb = order.get(i).getSecondElement();
			ArrayList<Couple> list = new ArrayList<Couple>();

			for (int j = 0; j < order.size(); j++) {

				if (bbb == order.get(j).getFirstElement()) {
					list.add(order.get(j));
				}
			}
			if (!list.isEmpty()) {
				for (int j = 0; j < list.size(); j++) {

					String ccc = list.get(j).getSecondElement();
					Couple coppia = new Couple(aaa, ccc);

					// Adds the precedence only if it doesn't exist
					if (!exist(coppia))
						order.add(coppia);

				}
			}

		}

	}

	public void applyRuleRdfs5_2() {

		for (int i = 0; i < order.size(); i++) {
			String bbb = order.get(i).getFirstElement();
			String ccc = order.get(i).getSecondElement();
			ArrayList<Couple> list = new ArrayList<Couple>();

			for (int j = 0; j < order.size(); j++) {

				if (bbb == order.get(j).getSecondElement()) {
					list.add(order.get(j));

				}
			}
			if (!list.isEmpty()) {
				for (int j = 0; j < list.size(); j++) {

					String aaa = list.get(j).getFirstElement();
					Couple coppia = new Couple(aaa, ccc);

				// Adds the precedence only if it doesn't exist
					if (!exist(coppia))
						order.add(coppia);

				}
			}
		}

	}

	public boolean exist(Couple cop) {

		boolean match = false;
		for (int i = 0; i < order.size(); i++) {

			if (order.get(i).getFirstElement() == cop.getFirstElement()
					&& order.get(i).getSecondElement() == cop
							.getSecondElement())
				match = true;
		}

		return match;
	}
	
	public Resource glb(Resource a, Resource b) {
		if(a.equals(b))
			return a;
		for (int i = 0; i < order.size(); i++) {
			if(a.equals(order.get(i).getFirstElement())&& b.equals(order.get(i).getSecondElement()))
				return a;
			if(b.equals(order.get(i).getFirstElement())&& a.equals(order.get(i).getSecondElement()))
				return b;
		}
		return null;
	}
	
	public void output(){
		for (int i = 0; i < order.size(); i++) {
		System.out.println(order.get(i).getFirstElement()+"  "+order.get(i).getSecondElement());
		}
	}

}
