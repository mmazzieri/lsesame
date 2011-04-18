package it.univpm.diiga.lsesame.comparators;

import it.univpm.diiga.lsesame.ResourceComparator;

import org.openrdf.model.Resource;

public class ContextComparerFirstExample implements ResourceComparator {

	/**
	 * In ContextComparerFirstExample the partially ordered set is the follow
	 * top / / \ \ a b c d The greater lower bound between incomparable contexts
	 * is the null context
	 */
	public Resource glb(Resource a, Resource b) {
		if (a == null) {
			return b;
		} else if (b == null) {
			return a;
		} else {
			// Two incomparable contexts
			return null;
		}
	}
}
