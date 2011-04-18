package it.univpm.diiga.lsesame.comparators;

import org.openrdf.model.Resource;

import it.univpm.diiga.lsesame.ResourceComparator;

public class ContextComparerSecondExample implements ResourceComparator {
	
	 // In ContextComparerFirstExample the partially ordered set is the follow
	 // 								top 
	 // 								/ \ 
	 //							   a   b 
	 //                        |   | 
	 //                        c   d 
	 //The greater lower bound between the incomparable contexts is a null context(i.e. a-b,c-d).
	 

	
	 // The partially ordered set above is stored in a matrix.
	 //The elements a, b, c, d represent four generic URI
	 
	private String order[][] = {
			{ "a", "b" },
			{ "c", "d" } };


	public Resource glb(Resource a, Resource b) {
		int pos_a_i = 0, pos_b_i = 0, pos_a_j = 0, pos_b_j = 0;
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				if (order[i][j].equals(a)) {
					pos_a_i = i;
					pos_a_j = j;
				}
				if (order[i][j].equals(b)) {
					pos_b_i = i;
					pos_b_j = j;
				}
			}
		}

		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
			//Two incomparable contexts
		} else if (pos_a_i == pos_b_i
				|| (pos_a_i != pos_b_i && pos_a_j != pos_b_j)) {
			return null;
		} else if (pos_a_i > pos_b_i && pos_a_j == pos_b_j) {
			return a;
		} else if (pos_a_i < pos_b_i && pos_a_j == pos_b_j) {
			return b;
		} else
			return null;

	}

}
