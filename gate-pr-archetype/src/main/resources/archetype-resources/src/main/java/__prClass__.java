package ${package};

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.*;

import org.apache.log4j.Logger;

@CreoleResource(
    #if( $prName != "" )name = "$prName"#set($comma=",")#end#if( $prComment != "" )$!comma
    comment = "$prComment"#set($comma=",")#end
)
public class ${prClass} extends AbstractLanguageAnalyser {

  private static final Logger log = Logger.getLogger(${prClass}.class);

  /**
   * Annotation set name from which this PR will take its input annotations.
   */
  private String inputASName;

  /**
   * Annotation set name into which this PR will create new annotations.
   */
  private String outputASName;


  public String getInputASName() {
    return inputASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "The annotation set used for input annotations")
  public void setInputASName(String inputASName) {
    this.inputASName = inputASName;
  }

  public String getOutputASName() {
    return outputASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "The annotation set used for output annotations")
  public void setOutputASName(String outputASName) {
    this.outputASName = outputASName;
  }

  /**
   * Initialize this #if( $prName != "" )${prName}#{else}${prClass}#end.
   * @return this resource.
   * @throws ResourceInstantiationException if an error occurs during init.
   */
  public Resource init() throws ResourceInstantiationException {
    log.debug("#if( $prName != "" )${prName}#{else}${prClass}#end is initializing");

    // your initialization code here

    return this;
  }

  /**
   * Execute this #if( $prName != "" )${prName}#{else}${prClass}#end over the current document.
   * @throws ExecutionException if an error occurs during processing.
   */
  public void execute() throws ExecutionException {
    // check the interrupt flag before we start - in a long-running PR it is
    // good practice to check this flag at appropriate key points in the
    // execution, to allow the user to interrupt processing if it takes too
    // long.
    if(isInterrupted()) {
      throw new ExecutionInterruptedException("Execution of #if( $prName != "" )${prName}#{else}${prClass}#end has been interrupted!");
    }
    interrupted = false;

    Document doc = getDocument();
    if(doc != null) {
      AnnotationSet inputAS = doc.getAnnotations(inputASName);
      AnnotationSet outputAS = doc.getAnnotations(outputASName);

      // do your processing here - take annotations from the inputAS and put
      // results into the outputAS
    }
  }

}

