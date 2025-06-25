import java.util.*;
import java.io.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileUtils;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

public class ctDTSreasoner 
{
    private static File ctDTSfile = new File("./KB/ctDTS.ttl");
    private static File SOAfile = new File("./KB/Examples/Example1.ttl");
    private static File inferredTriplesFile = new File("./KB/inferredABox.ttl");
    
    public static void main(String[] args) throws Exception 
    {
        if(args.length==3)
        {
            ctDTSfile = new File(args[0]);
            SOAfile = new File(args[1]);
            inferredTriplesFile = new File(args[2]);
        }
        
            //The two models also include the inference rules. 
            //There are rules both in the Deontic Traditional Scheme (DTS) 
            //and in the State of Affairs (SOA).
        Model TBox = ModelFactory.createDefaultModel(); 
        FileInputStream fis = new FileInputStream(ctDTSfile);
        TBox.read(fis, "urn:dummy", FileUtils.langTurtle);
        fis.close();
        
        fis = new FileInputStream(SOAfile);
        Model ABox = ModelFactory.createDefaultModel(); 
        ABox.read(fis, "urn:dummy", FileUtils.langTurtle);
        fis.close();
        
        Model model = ModelFactory.createDefaultModel();
        model.add(TBox);
        model.add(ABox);
        
            //We take the rules and we re-execute them on the model until they do not infer any more triple.
        ArrayList<String> inferenceRules = extractAllInferenceRules(model);
        long lastSize = 0;
        while(model.size()>lastSize)
        {
            lastSize = model.size();
            for(String inferenceRule:inferenceRules)
            {
                Query qry = QueryFactory.create(inferenceRule);
                QueryExecution qe1 = QueryExecutionFactory.create(qry, model);
                model.add(qe1.execConstructDataset().getDefaultModel().listStatements().toList());
            }
            
            //print(model, tboxStatements, inferredTriplesFile);
            model=model;
        }
                
        print(model, TBox, ABox, inferredTriplesFile);
    }

    private static ArrayList<String> extractAllInferenceRules(Model model)
    {
            //In front of each SPARQL, we need to attach the mapping of the prefixes.
        String prefixes = "";
        Map<String,String> prefixMap = model.getNsPrefixMap();
        ArrayList<String> keys = new ArrayList<String>(prefixMap.keySet());
        for(String key:keys)prefixes+="PREFIX "+key+": <"+prefixMap.get(key)+"> \n";

        ArrayList<String> ret = new ArrayList<String>();
        NodeIterator ni = model.listObjectsOfProperty(model.getProperty("https://w3id.org/ontology/conflict-tolerantdeontictraditionalscheme#has-sparql-code"));
        while(ni.hasNext())ret.add(prefixes+ni.next().asLiteral().toString());
        return ret;
    }
    
    public static void print(Model model, Model TBox, Model ABox, File inferredTriplesFile)throws Exception
    {
            //We output the inferred statements minus the useless ones.
        List<Statement> finalStatements = model.listStatements().toList();
        finalStatements.removeAll(identifyUselessStatements(model, TBox, ABox));
        
        Model outputModel = ModelFactory.createDefaultModel();
        outputModel.setNsPrefixes(model.getNsPrefixMap());
        outputModel.add(finalStatements);
        FileOutputStream outputStream = new FileOutputStream(inferredTriplesFile);
        RDFDataMgr.write(outputStream, outputModel, RDFFormat.TURTLE_BLOCKS);
        outputStream.close();      
    }
    
        //The key inferences of the proposed framework concern contradictions, conflicts, compliance, and violations: the goal is to determine 
        //whether the knowledge base is inconsistent or conflicting, and whether norms have been complied with or violated.
        //Some inferred statements are not relevant to these core derivations, so including them in the output file would be unnecessarily verbose.
        //This method identifies such statements and returns them so that the previous method can remove them.
    public static List<Statement> identifyUselessStatements(Model model, Model TBox, Model ABox)throws Exception
    {
            //TBox statements are the first to be removed. We want only the inferred statements plus the one in the ABox.
        ArrayList<Statement> toRemove = new ArrayList<Statement>();
        
        toRemove.addAll(TBox.listStatements().toList());
        
            //Sometimes Apache Jena adds the strings of the inference rules in the output model, as "rdf:type xsd:string".
            //We remove them. NB. yes, I said that it does it only "sometimes"... which is very bad, it should either do
            //it always or never...
        model.listStatements().forEachRemaining((var stmt) -> {
            if(stmt.getSubject().isLiteral()&&stmt.getPredicate().equals(RDF.type)&&stmt.getObject().equals(XSD.xstring))
            {
                String subject = stmt.getSubject().toString();
                subject = subject.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").replaceAll("\"", "");
                if(subject.startsWith("CONSTRUCT"))toRemove.add(stmt);
            }
        });
        
            //These are just rdfs:subClassOf, rdfs:Class, modalities, etc. Not very interesting statements.
        toRemove.addAll(model.listStatements(null, model.getProperty("https://w3id.org/ontology/conflict-tolerantdeontictraditionalscheme#has-sparql-code"), (RDFNode)null).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, model.getResource("https://w3id.org/ontology/conflict-tolerantdeontictraditionalscheme#Modality")).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, model.getResource("https://w3id.org/ontology/conflict-tolerantdeontictraditionalscheme#InferenceRule")).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, model.getResource("https://w3id.org/ontology/conflict-tolerantdeontictraditionalscheme#Eventuality")).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, RDF.Statement).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, RDFS.Class).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, RDF.Statement).toList());
        toRemove.addAll(model.listStatements(null, RDF.rest, (RDFNode)null).toList());
        toRemove.addAll(model.listStatements(null, RDF.first, (RDFNode)null).toList());
        toRemove.addAll(model.listStatements(null, model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#List"), (RDFNode)null).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, RDF.Property).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, model.getResource("https://w3id.org/ontology/conflict-tolerantdeontictraditionalscheme#ThematicRole")).toList());
        toRemove.addAll(model.listStatements(null, RDF.type, model.getResource("https://w3id.org/ontology/conflict-tolerantdeontictraditionalscheme#EventualityType")).toList());
        toRemove.addAll(model.listStatements(null, RDFS.subPropertyOf, (RDFNode)null).toList());
        
        /**/
        
        return toRemove;
    }
}