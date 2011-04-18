/*
 * Derived from Sesame, copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package it.univpm.diiga.lsesame;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * Constants for RDF Schema primitives and for the RDF Schema namespace.
 */
public class LabeledRDFS {

	/** http://www.w3.org/2000/01/rdf-schema# */
	public static final String NAMESPACE = "http://www.informaticard.it/progetti-di-ricerca/labeled-logic#";

	/** http://www.informaticard.it/progetti-di-ricerca/labeled-logic#Precedes */
	public final static URI PRECEDES;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		PRECEDES = factory.createURI(LabeledRDFS.NAMESPACE, "Precedes");
	}
}