package it.univpm.diiga.lsesame.comparators;

import org.openrdf.model.Resource;
import org.openrdf.model.impl.URIImpl;

public class ContextComparerThirdExample {

	/**
	 * The partially ordered set above is stored in a matrix.
	 * The elements a, b, c, d represent four generic URI
	 */
	private Resource order[][] = { { new URIImpl("a"), new URIImpl("b") }, 
			{ new URIImpl("c"), new URIImpl("d") } };

	public Resource glb(Resource a, Resource b) {
		int pos_a_i = 0, pos_b_i = 0, pos_a_j = 0, pos_b_j = 0;
		// It retrieves the index position of the URI in the matrix
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
		// With the index position of the URI it can obtain the glb between two
		// contexts
		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
		} else if (pos_a_i == pos_b_i) { // Two incomparable contexts
			return null;
		} else if (pos_a_i > pos_b_i && pos_a_j == pos_b_j) {
			return a;
		} else if (pos_a_i < pos_b_i && pos_a_j == pos_b_j) {
			return b;
		} else if (pos_a_i != pos_b_i && pos_a_j != pos_b_j) {
			if (pos_a_i > pos_b_i)
				return a;
			else
				return b;

		} else
			return null;

	}

}
