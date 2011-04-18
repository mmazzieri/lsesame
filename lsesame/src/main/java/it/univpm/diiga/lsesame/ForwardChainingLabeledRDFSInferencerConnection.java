/*
 * Derived from Sesame, copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package it.univpm.diiga.lsesame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.text.ASCIIUtil;
import org.openrdf.cursor.Cursor;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnectionListener;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.inferencer.helpers.InferencerConnectionWrapper;
import org.openrdf.store.StoreException;

/**
 * Forward-chaining RDF Schema inferencer, using the rules from the <a
 * href="http://www.w3.org/TR/2004/REC-rdf-mt-20040210/">RDF Semantics
 * Recommendation (10 February 2004)</a>. This inferencer can be used to add RDF
 * Schema semantics to any Sail that returns {@link InferencerConnection}s from
 * their {@link Sail#getConnection()} method.
 */
class ForwardChainingLabeledRDFSInferencerConnection extends InferencerConnectionWrapper implements
        SailConnectionListener {

    /*-----------*blank
      * Constants *
      *-----------*/

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /*-----------*
      * Variables *
      *-----------*/

    /**
     * true if the base Sail reported removed statements.
     */
    private boolean statementsRemoved;

    /**
     * Contains the statements that have been reported by the base Sail as
     */
    private Model newStatements;

    private Model newThisIteration;

    /**
     * Flags indicating which rules should be evaluated.
     */
    private boolean[] checkRule = new boolean[RDFSRules.RULECOUNT];

    /**
     * Flags indicating which rules should be evaluated next iteration.
     */
    private boolean[] checkRuleNextIter = new boolean[RDFSRules.RULECOUNT];

    /**
     * The number of inferred statements per rule.
     */
    private int[] ruleCount = new int[RDFSRules.RULECOUNT];

    // The context comparer used to compare contexts

    ResourceComparator comparator;

    /*--------------*
      * Constructors *
      *--------------*/

    public ForwardChainingLabeledRDFSInferencerConnection(InferencerConnection con) {
        super(con);
        con.addConnectionListener(this);
        comparator = new ContextComparator(con);
    }

    /*---------*
      * Methods *
      *---------*/

    // Called by base sail
    public void statementAdded(Statement st) {
        if (statementsRemoved) {
            // No need to record, starting from scratch anyway
            return;
        }

        if (newStatements == null) {
            newStatements = new LinkedHashModel();
        }
        newStatements.add(st);
    }

    // Called by base sail
    public void statementRemoved(Statement st) {
        statementsRemoved = true;
        newStatements = null;
    }

    @Override
    public void flushUpdates()
            throws StoreException {
        super.flushUpdates();

        if (statementsRemoved) {
            logger.debug("statements removed, starting inferencing from scratch");
            removeInferredStatements(null, null, null);
            logger.debug("Inserting axiom statements");
            addAxiomStatements(this);

            newStatements = new LinkedHashModel();
            Cursor<? extends Statement> cursor = getDelegate().getStatements(null, null, null, true);
            Statement st;
            while ((st = cursor.next()) != null) {
                newStatements.add(st);
            }

            statementsRemoved = false;
        }

        doInferencing();
    }

    @Override
    public void rollback()
            throws StoreException {
        super.rollback();

        statementsRemoved = false;
        newStatements = null;
    }

    /**
     * Adds all basic set of axiom statements from which the complete set can be
     * inferred to the underlying Sail.
     * @param con an inferences connection
     * @throws org.openrdf.store.StoreException if one of the axiomatic statements could not be added
     */
    protected static void addAxiomStatements(InferencerConnection con)
            throws StoreException {
        // RDF axiomatic triples (from RDF Semantics, section 3.1):

        con.addInferredStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
        con.addInferredStatement(RDF.SUBJECT, RDF.TYPE, RDF.PROPERTY);
        con.addInferredStatement(RDF.PREDICATE, RDF.TYPE, RDF.PROPERTY);
        con.addInferredStatement(RDF.OBJECT, RDF.TYPE, RDF.PROPERTY);

        con.addInferredStatement(RDF.FIRST, RDF.TYPE, RDF.PROPERTY);
        con.addInferredStatement(RDF.REST, RDF.TYPE, RDF.PROPERTY);
        con.addInferredStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY);

        con.addInferredStatement(RDF.NIL, RDF.TYPE, RDF.LIST);

        // RDFS axiomatic triples (from RDF Semantics, section 4.1):

        con.addInferredStatement(RDF.TYPE, RDFS.DOMAIN, RDFS.RESOURCE);
        con.addInferredStatement(RDFS.DOMAIN, RDFS.DOMAIN, RDF.PROPERTY);
        con.addInferredStatement(RDFS.RANGE, RDFS.DOMAIN, RDF.PROPERTY);
        con.addInferredStatement(RDFS.SUBPROPERTYOF, RDFS.DOMAIN, RDF.PROPERTY);
        con.addInferredStatement(RDFS.SUBCLASSOF, RDFS.DOMAIN, RDFS.CLASS);
        con.addInferredStatement(RDF.SUBJECT, RDFS.DOMAIN, RDF.STATEMENT);
        con.addInferredStatement(RDF.PREDICATE, RDFS.DOMAIN, RDF.STATEMENT);
        con.addInferredStatement(RDF.OBJECT, RDFS.DOMAIN, RDF.STATEMENT);
        con.addInferredStatement(RDFS.MEMBER, RDFS.DOMAIN, RDFS.RESOURCE);
        con.addInferredStatement(RDF.FIRST, RDFS.DOMAIN, RDF.LIST);
        con.addInferredStatement(RDF.REST, RDFS.DOMAIN, RDF.LIST);
        con.addInferredStatement(RDFS.SEEALSO, RDFS.DOMAIN, RDFS.RESOURCE);
        con.addInferredStatement(RDFS.ISDEFINEDBY, RDFS.DOMAIN, RDFS.RESOURCE);
        con.addInferredStatement(RDFS.COMMENT, RDFS.DOMAIN, RDFS.RESOURCE);
        con.addInferredStatement(RDFS.LABEL, RDFS.DOMAIN, RDFS.RESOURCE);
        con.addInferredStatement(RDF.VALUE, RDFS.DOMAIN, RDFS.RESOURCE);

        con.addInferredStatement(RDF.TYPE, RDFS.RANGE, RDFS.CLASS);
        con.addInferredStatement(RDFS.DOMAIN, RDFS.RANGE, RDFS.CLASS);
        con.addInferredStatement(RDFS.RANGE, RDFS.RANGE, RDFS.CLASS);
        con.addInferredStatement(RDFS.SUBPROPERTYOF, RDFS.RANGE, RDF.PROPERTY);
        con.addInferredStatement(RDFS.SUBCLASSOF, RDFS.RANGE, RDFS.CLASS);
        con.addInferredStatement(RDF.SUBJECT, RDFS.RANGE, RDFS.RESOURCE);
        con.addInferredStatement(RDF.PREDICATE, RDFS.RANGE, RDFS.RESOURCE);
        con.addInferredStatement(RDF.OBJECT, RDFS.RANGE, RDFS.RESOURCE);
        con.addInferredStatement(RDFS.MEMBER, RDFS.RANGE, RDFS.RESOURCE);
        con.addInferredStatement(RDF.FIRST, RDFS.RANGE, RDFS.RESOURCE);
        con.addInferredStatement(RDF.REST, RDFS.RANGE, RDF.LIST);
        con.addInferredStatement(RDFS.SEEALSO, RDFS.RANGE, RDFS.RESOURCE);
        con.addInferredStatement(RDFS.ISDEFINEDBY, RDFS.RANGE, RDFS.RESOURCE);
        con.addInferredStatement(RDFS.COMMENT, RDFS.RANGE, RDFS.LITERAL);
        con.addInferredStatement(RDFS.LABEL, RDFS.RANGE, RDFS.LITERAL);
        con.addInferredStatement(RDF.VALUE, RDFS.RANGE, RDFS.RESOURCE);

        con.addInferredStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.CONTAINER);
        con.addInferredStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.CONTAINER);
        con.addInferredStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.CONTAINER);
        con.addInferredStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);

        con.addInferredStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);

        con.addInferredStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.DATATYPE);
        con.addInferredStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
        con.addInferredStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.CLASS);
    }

    protected void doInferencing()
            throws StoreException {
        if (!hasNewStatements()) {
            // There's nothing to do
            return;
        }

        // initialize some vars
        int totalInferred = 0;
        int iteration = 0;
        int nofInferred;

        // All rules need to be checked:
        for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
            ruleCount[i] = 0;
            checkRuleNextIter[i] = true;
        }

        while (hasNewStatements()) {
            iteration++;
            logger.debug("starting iteration " + iteration);
            prepareIteration();

            nofInferred = 0;
            nofInferred += applyRule(RDFSRules.Rdf1);
            nofInferred += applyRule(RDFSRules.Rdfs2_1);
            nofInferred += applyRule(RDFSRules.Rdfs2_2);
            nofInferred += applyRule(RDFSRules.Rdfs3_1);
            nofInferred += applyRule(RDFSRules.Rdfs3_2);
            nofInferred += applyRule(RDFSRules.Rdfs4a);
            nofInferred += applyRule(RDFSRules.Rdfs4b);
            nofInferred += applyRule(RDFSRules.Rdfs5_1);
            nofInferred += applyRule(RDFSRules.Rdfs5_2);
            nofInferred += applyRule(RDFSRules.Rdfs6);
            nofInferred += applyRule(RDFSRules.Rdfs7_1);
            nofInferred += applyRule(RDFSRules.Rdfs7_2);
            nofInferred += applyRule(RDFSRules.Rdfs8);
            nofInferred += applyRule(RDFSRules.Rdfs9_1);
            nofInferred += applyRule(RDFSRules.Rdfs9_2);
            nofInferred += applyRule(RDFSRules.Rdfs10);
            nofInferred += applyRule(RDFSRules.Rdfs11_1);
            nofInferred += applyRule(RDFSRules.Rdfs11_2);
            nofInferred += applyRule(RDFSRules.Rdfs12);
            nofInferred += applyRule(RDFSRules.Rdfs13);
            nofInferred += applyRule(RDFSRules.RX1);

            logger.debug("iteration " + iteration + " done; inferred " + nofInferred + " new statements");
            totalInferred += nofInferred;
        }

        // Print some statistics
        logger.debug("---RdfMTInferencer statistics:---");
        logger.debug("total statements inferred = " + totalInferred);
        for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
            logger.debug("rule " + RDFSRules.RULENAMES[i] + ":\t#inferred=" + ruleCount[i]);
        }
        logger.debug("---end of statistics:---");
    }

    protected void prepareIteration() {
        for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
            checkRule[i] = checkRuleNextIter[i];

            // reset for next iteration:
            checkRuleNextIter[i] = false;
        }

        newThisIteration = newStatements;
        newStatements = new LinkedHashModel();
    }

    protected boolean hasNewStatements() {
        return newStatements != null && !newStatements.isEmpty();
    }

    protected void updateTriggers(int ruleNo, int nofInferred) {
        if (nofInferred > 0) {
            ruleCount[ruleNo] += nofInferred;

            // Check which rules are triggered by this one.
            boolean[] triggers = RDFSRules.TRIGGERS[ruleNo];

            for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
                if (triggers[i]) {
                    checkRuleNextIter[i] = true;
                }
            }
        }
    }

    protected int applyRule(int rule)
            throws StoreException {
        if (!checkRule[rule]) {
            return 0;
        }

        int nofInferred;
        nofInferred = applyRuleInternal(rule);
        updateTriggers(rule, nofInferred);
        return nofInferred;
    }

    protected int applyRuleInternal(int rule)
            throws StoreException {
        int result = 0;

        switch (rule) {
            case RDFSRules.Rdf1:
                result = applyRuleLabeledRdf1();
                break;
            case RDFSRules.Rdfs2_1:
                result = applyRuleLabeledRdfs2_1();
                break;
            case RDFSRules.Rdfs2_2:
                result = applyRuleLabeledRdfs2_2();
                break;
            case RDFSRules.Rdfs3_1:
                result = applyRuleLabeledRdfs3_1();
                break;
            case RDFSRules.Rdfs3_2:
                result = applyRuleLabeledRdfs3_2();
                break;
            case RDFSRules.Rdfs4a:
                result = applyRuleLabeledRdfs4a();
                break;
            case RDFSRules.Rdfs4b:
                result = applyRuleLabeledRdfs4b();
                break;
            case RDFSRules.Rdfs5_1:
                result = applyRuleLabeledRdfs5_1();
                break;
            case RDFSRules.Rdfs5_2:
                result = applyRuleLabeledRdfs5_2();
                break;
            case RDFSRules.Rdfs6:
                result = applyRuleLabeledRdfs6();
                break;
            case RDFSRules.Rdfs7_1:
                result = applyRuleLabeledRdfs7_1();
                break;
            case RDFSRules.Rdfs7_2:
                result = applyRuleLabeledRdfs7_2();
                break;
            case RDFSRules.Rdfs8:
                result = applyRuleLabeledRdfs8();
                break;
            case RDFSRules.Rdfs9_1:
                result = applyRuleLabeledRdfs9_1();
                break;
            case RDFSRules.Rdfs9_2:
                result = applyRuleLabeledRdfs9_2();
                break;
            case RDFSRules.Rdfs10:
                result = applyRuleLabeledRdfs10();
                break;
            case RDFSRules.Rdfs11_1:
                result = applyRuleLabeledRdfs11_1();
                break;
            case RDFSRules.Rdfs11_2:
                result = applyRuleLabeledRdfs11_2();
                break;
            case RDFSRules.Rdfs12:
                result = applyRuleLabeledRdfs12();
                break;
            case RDFSRules.Rdfs13:
                result = applyRuleLabeledRdfs13();
                break;
            case RDFSRules.RX1:
                result = applyRuleX1();
                break;
            default:
                // FIXME throw exception here?
                break;
        }
        // ThreadLog.trace("Rule " + RDFSRules.RULENAMES[rule] + " inferred " +
        // result + " new triples.");
        return result;
    }

    // i: xxx aaa yyy --> i: aaa rdf:type rdf:Property
    private int applyRuleLabeledRdf1()
            throws StoreException {
        int nofInferred = 0;

        for (Object aNewThisIteration : newThisIteration) {
            Statement st = (Statement) aNewThisIteration;

            boolean added = addInferredStatement(st.getPredicate(), RDF.TYPE, RDF.PROPERTY, st.getContext());

            if (added) {
                nofInferred++;
            }
        }

        return nofInferred;
    }

    // i: xxx aaa yyy (nt) && j: aaa rdfs:domain zzz (t1) --> k: xxx rdf:type zzz
    // where k=glb(i, j)
    private int applyRuleLabeledRdfs2_1()
            throws StoreException {
        int nofInferred = 0;

        for (Object aNewThisIteration : newThisIteration) {
            Statement nt = (Statement) aNewThisIteration;

            Resource xxx = nt.getSubject();
            URI aaa = nt.getPredicate();
            Resource i = nt.getContext();

            Cursor<? extends Statement> t1Iter;
            t1Iter = getDelegate().getStatements(aaa, RDFS.DOMAIN, null, true);

            Statement t1;
            while ((t1 = t1Iter.next()) != null) {

                Value zzz = t1.getObject();
                Resource j = t1.getContext();
                if (zzz instanceof Resource) {
                    boolean added = addInferredStatement(xxx, RDF.TYPE, zzz, comparator.glb(i, j));
                    if (added) {
                        nofInferred++;
                    }
                }
            }

            t1Iter.close();
        }

        return nofInferred;
    }

    // i: aaa rdfs:domain zzz (nt) && j: xxx aaa yyy (t1) --> k: xxx rdf:type zzz
    // where k=glb(i, j)
    private int applyRuleLabeledRdfs2_2()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.DOMAIN, null)) {
            Statement nt = (Statement) o;

            Resource aaa = nt.getSubject();
            Value zzz = nt.getObject();
            Resource i = nt.getContext();

            if (aaa instanceof URI && zzz instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements(null, (URI) aaa, null, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Resource xxx = t1.getSubject();
                    Resource j = t1.getContext();
                    boolean added = addInferredStatement(xxx, RDF.TYPE, zzz, comparator.glb(i, j));
                    if (added) {
                        nofInferred++;
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;
    }

    // i: xxx aaa uuu (nt) && j: aaa rdfs:range zzz (t1) --> k: uuu rdf:type zzz
    // where k = glb(i, j)
    private int applyRuleLabeledRdfs3_1()
            throws StoreException {
        int nofInferred = 0;

        for (Object aNewThisIteration : newThisIteration) {
            Statement nt = (Statement) aNewThisIteration;

            URI aaa = nt.getPredicate();
            Value uuu = nt.getObject();
            Resource i = nt.getContext();

            if (uuu instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements(aaa, RDFS.RANGE, null, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Value zzz = t1.getObject();
                    Resource j = t1.getContext();
                    if (zzz instanceof Resource) {
                        boolean added = addInferredStatement((Resource) uuu, RDF.TYPE, zzz, comparator.glb(i, j));
                        if (added) {
                            nofInferred++;
                        }
                    }
                }
                t1Iter.close();
            }
        }
        return nofInferred;
    }

    // i: aaa rdfs:range zzz (nt) && j: xxx aaa uuu (t1) --> k: uuu rdf:type zzz
    // where k = glb(i, j)
    private int applyRuleLabeledRdfs3_2()

            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.RANGE, null)) {
            Statement nt = (Statement) o;

            Resource aaa = nt.getSubject();
            Value zzz = nt.getObject();
            Resource i = nt.getContext();

            if (aaa instanceof URI && zzz instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements(null, (URI) aaa, null, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Value uuu = t1.getObject();
                    Resource j = t1.getContext();
                    if (uuu instanceof Resource) {
                        boolean added = addInferredStatement((Resource) uuu, RDF.TYPE, zzz, comparator.glb(i, j));
                        if (added) {
                            nofInferred++;
                        }
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;

    }

    // i: xxx aaa yyy --> i: xxx rdf:type rdfs:Resource
    private int applyRuleLabeledRdfs4a()
            throws StoreException {
        int nofInferred = 0;

        for (Object aNewThisIteration : newThisIteration) {
            Statement st = (Statement) aNewThisIteration;

            boolean added = addInferredStatement(st.getSubject(), RDF.TYPE, RDFS.RESOURCE, st.getContext());
            if (added) {
                nofInferred++;
            }
        }

        return nofInferred;
    }

    // i: xxx aaa uuu --> i: uuu rdf:type rdfs:Resource
    private int applyRuleLabeledRdfs4b()
            throws StoreException {
        int nofInferred = 0;

        for (Object aNewThisIteration : newThisIteration) {
            Statement st = (Statement) aNewThisIteration;

            Value uuu = st.getObject();
            if (uuu instanceof Resource) {
                boolean added = addInferredStatement((Resource) uuu, RDF.TYPE, RDFS.RESOURCE, st.getContext());
                if (added) {
                    nofInferred++;
                }
            }
        }

        return nofInferred;
    }

    // i: aaa rdfs:subPropertyOf bbb (nt) && j: bbb rdfs:subPropertyOf ccc (t1) --> k: aaa rdfs:subPropertyOf ccc where k = glb(i, j)
    private int applyRuleLabeledRdfs5_1()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.SUBPROPERTYOF, null)) {
            Statement nt = (Statement) o;

            Resource aaa = nt.getSubject();
            Value bbb = nt.getObject();
            Resource i = nt.getContext();

            if (bbb instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements((Resource) bbb, RDFS.SUBPROPERTYOF, null, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Value ccc = t1.getObject();
                    Resource j = t1.getContext();
                    if (ccc instanceof Resource) {
                        boolean added = addInferredStatement(aaa, RDFS.SUBPROPERTYOF, ccc, comparator.glb(i, j));
                        if (added) {
                            nofInferred++;
                        }
                    }
                }
                t1Iter.close();

            }
        }

        return nofInferred;
    }

    // i: bbb rdfs:subPropertyOf ccc (nt) && j: aaa rdfs:subPropertyOf bbb (t1) --> k: aaa rdfs:subPropertyOf ccc where k = glb(i, j)
    private int applyRuleLabeledRdfs5_2()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.SUBPROPERTYOF, null)) {
            Statement nt = (Statement) o;

            Resource bbb = nt.getSubject();
            Value ccc = nt.getObject();
            Resource i = nt.getContext();

            if (ccc instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements(null, RDFS.SUBPROPERTYOF, bbb, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Resource aaa = t1.getSubject();
                    Resource j = t1.getContext();
                    boolean added = addInferredStatement(aaa, RDFS.SUBPROPERTYOF, ccc, comparator.glb(i, j));
                    if (added) {
                        nofInferred++;
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;
    }


    // i: xxx rdf:type rdf:Property --> i: xxx rdfs:subPropertyOf xxx
    private int applyRuleLabeledRdfs6()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDF.TYPE, RDF.PROPERTY)) {
            Statement st = (Statement) o;

            Resource xxx = st.getSubject();
            boolean added = addInferredStatement(xxx, RDFS.SUBPROPERTYOF, xxx, st.getContext());
            if (added) {
                nofInferred++;
            }
        }

        return nofInferred;
    }


    // i: xxx aaa yyy (nt) && j: aaa rdfs:subPropertyOf bbb (t1) --> k: xxx bbb yyy where k = glb(i, j)
    private int applyRuleLabeledRdfs7_1()
            throws StoreException {
        int nofInferred = 0;

        for (Object aNewThisIteration : newThisIteration) {
            Statement nt = (Statement) aNewThisIteration;

            Resource xxx = nt.getSubject();
            URI aaa = nt.getPredicate();
            Value yyy = nt.getObject();
            Resource i = nt.getContext();

            Cursor<? extends Statement> t1Iter;
            t1Iter = getDelegate().getStatements(aaa, RDFS.SUBPROPERTYOF, null, true);

            Statement t1;
            while ((t1 = t1Iter.next()) != null) {

                Value bbb = t1.getObject();
                Resource j = t1.getContext();
                if (bbb instanceof URI) {
                    boolean added = addInferredStatement(xxx, (URI) bbb, yyy, comparator.glb(i, j));
                    if (added) {
                        nofInferred++;
                    }
                }
            }
            t1Iter.close();
        }

        return nofInferred;
    }

    // i: aaa rdfs:subPropertyOf bbb (nt) && j: xxx aaa yyy (t1) --> k: xxx bbb yyy where k = glb(i, j)
    private int applyRuleLabeledRdfs7_2()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.SUBPROPERTYOF, null)) {
            Statement nt = (Statement) o;

            Resource aaa = nt.getSubject();
            Value bbb = nt.getObject();
            Resource i = nt.getContext();

            if (aaa instanceof URI && bbb instanceof URI) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements(null, (URI) aaa, null, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Resource xxx = t1.getSubject();
                    Value yyy = t1.getObject();
                    Resource j = t1.getSubject();

                    boolean added = addInferredStatement(xxx, (URI) bbb, yyy, comparator.glb(i, j));
                    if (added) {
                        nofInferred++;
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;
    }

    // i: xxx rdf:type rdfs:Class --> i: xxx rdfs:subClassOf rdfs:Resource
    private int applyRuleLabeledRdfs8()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDF.TYPE, RDFS.CLASS)) {
            Statement st = (Statement) o;

            Resource xxx = st.getSubject();

            boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, RDFS.RESOURCE, st.getContext());
            if (added) {
                nofInferred++;
            }
        }

        return nofInferred;
    }

    // i: xxx rdfs:subClassOf yyy (nt) && j: aaa rdf:type xxx (t1) --> k: aaa rdf:type yyy where k = glb(i, j)
    private int applyRuleLabeledRdfs9_1()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.SUBCLASSOF, null)) {
            Statement nt = (Statement) o;

            Resource xxx = nt.getSubject();
            Value yyy = nt.getObject();
            Resource i = nt.getContext();

            if (yyy instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements(null, RDF.TYPE, xxx, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Resource aaa = t1.getSubject();
                    Resource j = t1.getContext();

                    boolean added = addInferredStatement(aaa, RDF.TYPE, yyy, comparator.glb(i, j));
                    if (added) {
                        nofInferred++;
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;
    }

    // i: aaa rdf:type xxx (nt) && xxx j: rdfs:subClassOf yyy (t1) --> k: aaa rdf:type yyy where k = glb(i, j)
    private int applyRuleLabeledRdfs9_2()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDF.TYPE, null)) {
            Statement nt = (Statement) o;

            Resource aaa = nt.getSubject();
            Value xxx = nt.getObject();
            Resource i = nt.getContext();

            if (xxx instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements((Resource) xxx, RDFS.SUBCLASSOF, null, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Value yyy = t1.getObject();
                    Resource j = t1.getContext();

                    if (yyy instanceof Resource) {
                        boolean added = addInferredStatement(aaa, RDF.TYPE, yyy, comparator.glb(i, j));
                        if (added) {
                            nofInferred++;
                        }
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;
    }

    // i: xxx rdf:type rdfs:Class --> i: xxx rdfs:subClassOf xxx
    private int applyRuleLabeledRdfs10()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDF.TYPE, RDFS.CLASS)) {
            Statement st = (Statement) o;

            Resource xxx = st.getSubject();

            boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, xxx, st.getContext());
            if (added) {
                nofInferred++;
            }
        }

        return nofInferred;
    }

    // i: xxx rdfs:subClassOf yyy (nt) && j: yyy rdfs:subClassOf zzz (t1) --> k: xxx rdfs:subClassOf zzz where k = glb(i, j)
    private int applyRuleLabeledRdfs11_1()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.SUBCLASSOF, null)) {
            Statement nt = (Statement) o;

            Resource xxx = nt.getSubject();
            Value yyy = nt.getObject();
            Resource i = nt.getContext();


            if (yyy instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements((Resource) yyy, RDFS.SUBCLASSOF, null, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Value zzz = t1.getObject();
                    Resource j = t1.getContext();

                    if (zzz instanceof Resource) {
                        boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, zzz, comparator.glb(i, j));
                        if (added) {
                            nofInferred++;
                        }
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;
    }

    // i: yyy rdfs:subClassOf zzz (nt) && j: xxx rdfs:subClassOf yyy (t1) --> k: xxx rdfs:subClassOf zzz where k = glb(i, j)
    private int applyRuleLabeledRdfs11_2()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDFS.SUBCLASSOF, null)) {
            Statement nt = (Statement) o;

            Resource yyy = nt.getSubject();
            Value zzz = nt.getObject();
            Resource i = nt.getSubject();

            if (zzz instanceof Resource) {
                Cursor<? extends Statement> t1Iter;
                t1Iter = getDelegate().getStatements(null, RDFS.SUBCLASSOF, yyy, true);

                Statement t1;
                while ((t1 = t1Iter.next()) != null) {

                    Resource xxx = t1.getSubject();
                    Resource j = t1.getContext();

                    boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, zzz, comparator.glb(i, j));
                    if (added) {
                        nofInferred++;
                    }
                }
                t1Iter.close();
            }
        }

        return nofInferred;
    }

    // i: xxx rdf:type rdfs:ContainerMembershipProperty --> i: xxx rdfs:subPropertyOf rdfs:member
    private int applyRuleLabeledRdfs12()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY)) {
            Statement st = (Statement) o;

            Resource xxx = st.getSubject();

            boolean added = addInferredStatement(xxx, RDFS.SUBPROPERTYOF, RDFS.MEMBER, st.getContext());
            if (added) {
                nofInferred++;
            }
        }

        return nofInferred;
    }

    // i: xxx rdf:type rdfs:Datatype --> i: xxx rdfs:subClassOf rdfs:Literal
    private int applyRuleLabeledRdfs13()
            throws StoreException {
        int nofInferred = 0;

        for (Object o : newThisIteration.filter(null, RDF.TYPE, RDFS.DATATYPE)) {
            Statement st = (Statement) o;

            Resource xxx = st.getSubject();


            boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, RDFS.LITERAL, st.getContext());
            if (added) {
                nofInferred++;
            }
        }

        return nofInferred;
    }

    // xxx rdf:_* yyy --> rdf:_* rdf:type rdfs:ContainerMembershipProperty
    // This is an extra rule for list membership properties (_1, _2, _3, ...).
    // The RDF MT does not specificy a production for this.
    private int applyRuleX1()
            throws StoreException {
        int nofInferred = 0;

        String prefix = RDF.NAMESPACE + "_";

        for (Object aNewThisIteration : newThisIteration) {
            Statement st = (Statement) aNewThisIteration;

            URI predNode = st.getPredicate();
            String predURI = predNode.toString();

            if (predURI.startsWith(prefix) && isValidPredicateNumber(predURI.substring(prefix.length()))) {
                boolean added = addInferredStatement(predNode, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY);
                if (added) {
                    nofInferred++;
                }
            }
        }

        return nofInferred;
    }

    /**
     * Util method for {@link #applyRuleX1}.
     *
     * @param str a string
     * @return whether the string represents a number
     */
    private boolean isValidPredicateNumber(String str) {
        int strLength = str.length();

        if (strLength == 0) {
            return false;
        }

        for (int i = 0; i < strLength; i++) {
            if (!ASCIIUtil.isNumber(str.charAt(i))) {
                return false;
            }
        }

        // No leading zeros
        return str.charAt(0) != '0';
    }
}
