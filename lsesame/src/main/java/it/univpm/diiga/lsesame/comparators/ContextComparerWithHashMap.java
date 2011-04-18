package it.univpm.diiga.lsesame.comparators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrdf.model.Resource;
import org.openrdf.model.impl.URIImpl;

public class ContextComparerWithHashMap {

	private HashMap<Resource, HashSet<Resource>> order;

	public ContextComparerWithHashMap() {
		Resource key;
		Resource value;
		order = new HashMap<Resource, HashSet<Resource>>();

		// Example with eight context precedences
		for (int i = 0; i < 8; i++) {
			switch (i) {
			case 0:
				key = new URIImpl("a");
				value = new URIImpl("b");
				break;
				
			case 1:
				key = new URIImpl("b");
				value = new URIImpl("c");
				break;
				
			case 2:
				key = new URIImpl("d");
				value = new URIImpl("c");
				break;
				
			case 3:
				key = new URIImpl("c");
				value = new URIImpl("f");
				break;
				
			case 4:
				key = new URIImpl("f");
				value = new URIImpl("e");
				break;
				
			case 5:
				key = new URIImpl("g");
				value = new URIImpl("a");
				break;
				
			case 6:
				key = new URIImpl("l");
				value = new URIImpl("d");
				break;
				
			case 7:
				key = new URIImpl("g");
				value = new URIImpl("l");
				break;
				
			default:
				key = null;
				value = null;
			}

			// Creation of a data structure for partially ordered set

			// If the key exists the method retrieves the HashSet mapped from
			// the
			// key
			// and add the value into the HashSet else it add the new key to
			// the map and create a new HashSet for the key and add the value
			// into
			// the HashSet.

			HashSet<Resource> set = order.get(key);
			if (set == null) {
				set = new HashSet<Resource>();
				set.add(value);
				order.put(key, set);
			}
			set.add(value);

			calculateClosure();

		}
	}

	public void calculateClosure() {

		// The method calculates the forward-chaining closure and creates the
		// map

		Resource[] keySet = order.keySet().toArray(new Resource[0]);

		for (Resource key : keySet) {
			HashSet<Resource> valueSet = order.get(key);

			Resource[] valueArray = valueSet.toArray(new Resource[0]);
			for (Resource value : valueArray) {
				for (Resource key2 : keySet) {

					if (key2.equals(value)) {
						HashSet<Resource> valueSet1 = order.get(key2);
						valueSet.addAll(valueSet1);
					}
				}
			}
		}

	}

	public Resource glb(Resource a, Resource b) {
		// If two contexts are equals the method returns one of the two contexts
		if (a.equals(b)) {
			return a;
		}
		if (order.containsKey(a)) {
			// If the 'b' context is contained in HashSet value mapped from the
			// key
			// 'a'
			// the method returns 'a'
			if (order.get(a).contains(b))
				return a;
		}
		if (order.containsKey(b)) {
			// If the 'a' context is contained in HashSet value mapped from the
			// key 'b'
			// the method returns 'b'
			if (order.get(b).contains(a))
				return b;
		}

		Resource[] keySet = order.keySet().toArray(new Resource[0]);

		for (Resource key : keySet) {
			HashSet<Resource> valueSet = order.get(key);
			if (valueSet.contains(a) && valueSet.contains(b)) {
				return key;
			}
		}

		// The method returns null if two contexts are incomparable
		return null;
	}

	public void output() {
		Set<Resource> keys = order.keySet();
		Iterator<Resource> iter1;
		Iterator<Resource> iter2;
		iter1 = keys.iterator();
		while (iter1.hasNext()) {
			Resource s = iter1.next();
			System.out.print(s + "-->");
			HashSet<Resource> valueSet;
			valueSet = order.get(s);
			iter2 = valueSet.iterator();
			while (iter2.hasNext()) {
				System.out.print(iter2.next());
			}
			System.out.println();
		}

	}
}
