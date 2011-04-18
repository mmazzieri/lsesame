/*
 * Derived from Sesame, copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package it.univpm.diiga.lsesame;

import org.openrdf.sail.Sail;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.openrdf.store.StoreConfigException;

/**
 * A {@link org.openrdf.sail.config.SailFactory} that creates {@link org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer}s
 * based on RDF configuration data.
 * 
 * @author Arjohn Kampman
 * @author Matteo Rossi
 * @author Mauro Mazzieri
 */
public class ForwardChainingLabeledRDFSInferencerFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see org.openrdf.sail.config.SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:ForwardChainingRDFSInferencerWithContext";

	/**
	 * Returns the Sail's type: <tt>openrdf:ForwardChainingRDFSInferencerWithContext</tt>.
	 */
	public String getSailType() {
		return SAIL_TYPE;
	}

	public SailImplConfig getConfig() {
		return new ForwardChainingRDFSInferencerConfig();
	}

	public Sail getSail(SailImplConfig config)
		throws StoreConfigException
	{
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new StoreConfigException("Invalid Sail type: " + config.getType());
		}

		return new ForwardChainingLabeledRDFSInferencer();
	}
}
