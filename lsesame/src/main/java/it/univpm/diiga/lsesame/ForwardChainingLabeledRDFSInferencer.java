/*
 * Derived from Sesame, copyright Aduna (http://www.aduna-software.com/) (c) 1997-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package it.univpm.diiga.lsesame;

import static java.lang.System.arraycopy;

import org.openrdf.sail.NotifyingSail;
import org.openrdf.sail.SailMetaData;
import org.openrdf.sail.helpers.NotifyingSailWrapper;
import org.openrdf.sail.helpers.SailMetaDataWrapper;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.inferencer.fc.config.ForwardChainingRDFSInferencerFactory;
import org.openrdf.sail.inferencer.helpers.AutoCommitInferencerConnection;
import org.openrdf.store.StoreException;

/**
 * Forward-chaining RDF Schema inferencer, using the rules from the <a
 * href="http://www.w3.org/TR/2004/REC-rdf-mt-20040210/">RDF Semantics
 * Recommendation (10 February 2004)</a>. This inferencer can be used to add RDF
 * Schema semantics to any Sail that returns {@link org.openrdf.sail.inferencer.InferencerConnection}s from
 * their {@link org.openrdf.sail.Sail#getConnection()} method.
 */
public class ForwardChainingLabeledRDFSInferencer extends NotifyingSailWrapper {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ForwardChainingLabeledRDFSInferencer() {
		super();
	}

	public ForwardChainingLabeledRDFSInferencer(NotifyingSail baseSail) {
		super(baseSail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public InferencerConnection getConnection()
		throws StoreException
	{
		try {
			InferencerConnection con = (InferencerConnection)super.getConnection();
			con = new ForwardChainingLabeledRDFSInferencerConnection(con);
			con = new AutoCommitInferencerConnection(con);
			return con;
		}
		catch (ClassCastException e) {
			throw new StoreException(e.getMessage(), e);
		}
	}

	/**
	 * Adds axiom statements to the underlying Sail.
	 */
	@Override
	public void initialize()
		throws StoreException
	{
		super.initialize();

		InferencerConnection con = getConnection();
		try {
			con.begin();
			ForwardChainingLabeledRDFSInferencerConnection.addAxiomStatements(con);
			con.commit();
		}
		finally {
			con.close();
		}
	}

	@Override
	public SailMetaData getMetaData()
		throws StoreException
	{
		return new SailMetaDataWrapper(super.getMetaData()) {

			@Override
			public String[] getReasoners() {
				String[] reasoners = super.getReasoners();
				String[] result = new String[reasoners.length + 1];
				result[reasoners.length] = ForwardChainingRDFSInferencerFactory.SAIL_TYPE;
				return result;
			}

			@Override
			public boolean isInferencing() {
				return true;
			}

			@Override
			public String[] getInferenceRules() {
				String[] rules = super.getInferenceRules();
				String[] result = new String[rules.length + RDFSRules.RULENAMES.length];
				arraycopy(RDFSRules.RULENAMES, 0, result, rules.length, RDFSRules.RULENAMES.length);
				return result;
			}

			@Override
			public boolean isHierarchicalInferencing() {
				return true;
			}

			@Override
			public boolean isRDFSInferencing() {
				return true;
			}
		};
	}
}
