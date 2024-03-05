import java.util.*;
import java.io.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileUtils;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

public class ctDTSreasoner 
{
    private static File ctDTSfile = new File("./ctDTS.ttl");
    private static File SOAfile = new File("./Examples/Example15.ttl");
    private static File inferredTriplesFile = new File("./inferredABox.rdf");
    
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
        Model model = ModelFactory.createDefaultModel(); 
        FileInputStream fis = new FileInputStream(ctDTSfile);
        model.read(fis, "urn:dummy", FileUtils.langTurtle);
        fis.close();
        List<Statement> tboxStatements = model.listStatements().toList();
        fis = new FileInputStream(SOAfile);
        model.read(fis, "urn:dummy", FileUtils.langTurtle);
        fis.close();
        
            //We take the rules and we re-execute them on the model until they do not infer any more triple.
        ArrayList<String> inferenceRules = extractAllInferenceRules(model);
        long lastSize = 0;
        while(model.size()>lastSize)
        {
            lastSize = model.size();
            
                //We execute all rules on the model while adding all inferred triples to the model.
            for(String inferenceRule:inferenceRules)
            {
                Query qry = QueryFactory.create(inferenceRule);
                QueryExecution qe1 = QueryExecutionFactory.create(qry, model);
                model.add(qe1.execConstructDataset().getDefaultModel().listStatements().toList());
            }
        }
        
        print(model, tboxStatements, inferredTriplesFile);
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
    
    private static void print(Model model, List<Statement> tboxStatements, File inferredTriplesFile)throws Exception
    {
            //We only output the inferred statements.
        List<Statement> finalStatements = model.listStatements().toList();
        finalStatements.removeAll(tboxStatements);
        Model outputModel = ModelFactory.createDefaultModel();
        outputModel.setNsPrefixes(model.getNsPrefixMap());
        outputModel.add(finalStatements);
        FileOutputStream outputStream = new FileOutputStream(inferredTriplesFile);
        RDFDataMgr.write(outputStream, outputModel, RDFFormat.TURTLE_BLOCKS);
        outputStream.close();        
    }
}