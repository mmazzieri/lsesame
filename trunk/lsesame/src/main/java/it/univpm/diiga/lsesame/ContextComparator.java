package it.univpm.diiga.lsesame;

import org.openrdf.cursor.Cursor;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.inferencer.helpers.InferencerConnectionWrapper;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.univpm.diiga.lsesame.comparators.ContextComparerFirstExample;

import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JOptionPane;

/**
 * @author Matteo Rossi
 * @author Mauro Mazzieri
 */
public class ContextComparator extends InferencerConnectionWrapper implements
		ResourceComparator {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private HashMap<Resource, HashSet<Resource>> order;

	public ContextComparator(InferencerConnection con) {
		// The InferencerConnection received as parameter is passed to
		// InferencerConnectionWrapper
		// that wraps this connection.
		// This connection is the same of the connection used at
		// ForwardChainingRDFSInferencerConnectionWithContext

		super(con);
		order = new HashMap<Resource, HashSet<Resource>>();

		Cursor<? extends Statement> Iter;
		try {

			// Retrieve all statements have 'label:\\precedes' property
			Iter = getDelegate().getStatements(null, LabeledRDFS.PRECEDES,
					null, true);

			Statement t1;
			while ((t1 = Iter.next()) != null) {

				Resource a = t1.getSubject();
				Value b = t1.getObject();

				if (b instanceof Resource) {
					// Creation of a data structure for partially ordered set.
					// If the key exists the method retrieves the HashSet mapped
					// from the key,
					// then puts the value into the HashSet; else, adds the new
					// key to
					// the map and creates a new HashSet for the key and add the
					// value into the HashSet.

					Resource value = (Resource) b;
					HashSet<Resource> set = order.get(a);
					if (set == null) {
						set = new HashSet<Resource>();
						set.add(value);
						order.put(a, set);
					}

					set.add(value);
					calculateClosure(a, value);
				}
			}

			Iter.close();
		} catch (StoreException e) {
			logger.debug("Error at initialization step in ContextComparator");
		}
	}

	public void calculateClosure(Resource key, Resource value) {

		// The method calculates the forward-chaining closure and creates the
		// map
		Resource[] keySet = order.keySet().toArray(
				new Resource[order.keySet().size()]);

		for (Resource k : keySet) {
			// Applies rule rdfs5.1
			if (k.equals(value)) {
				HashSet<Resource> valueSet = order.get(key);
				valueSet.addAll(order.get(k));
			}

			// Applies rule rdfs5.2
			HashSet<Resource> valueSet = order.get(k);

			Resource[] valueArray = valueSet.toArray(new Resource[valueSet
					.size()]);
			for (Resource v : valueArray) {
				if (key.equals(v)) {
					HashSet<Resource> valueSet1 = order.get(k);
					valueSet1.add(value);
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
			// key 'a'
			// the method returns 'a'
			if (order.get(a).contains(b)) {
				return a;
			}
		}

		if (order.containsKey(b)) {
			// If the 'a' context is contained in HashSet value mapped from the
			// key 'b'
			// the method returns 'b'
			if (order.get(b).contains(a)) {
				return b;
			}
		}

		// the contexts are incomparable
		return null;
	}

	public static void main(String[] args) {
		// A comparator for the first example
		ResourceComparator comparator = new ContextComparerFirstExample();

		// A comparator for the second example
		// SecondExample comparator = new SecondExample();

		// A comparator for the third example
		// ContextComparerThirdExample comparator = new
		// ContextComparerThirdExample();

		// A comparator with list
		// ContextComparerWithList comparator = new ContextComparerWithList();

		// A comparator with HashMap
		// ContextComparerWithHashMap comparator = new
		// ContextComparerWithHashMap();

		// A comparator with optimized HashMap
		// ContextComparator comparator = new ContextComparator();

		Resource a = new URIImpl(JOptionPane.showInputDialog("Insert first label"));
		Resource b = new URIImpl(JOptionPane.showInputDialog("Insert second label"));
		Resource min = comparator.glb(a, b);

		if (min != null) {
			System.out.println(min);
		} else {
			System.out.println("Null context");
		}
	}
}
