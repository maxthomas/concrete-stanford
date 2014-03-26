package edu.jhu.hlt.concrete.stanford;

import edu.jhu.hlt.concrete.Concrete;
import edu.jhu.hlt.concrete.*;
import edu.jhu.hlt.concrete.util.*;
import edu.jhu.hlt.concrete.agiga.AgigaConverter;
import edu.jhu.agiga.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * given a Communication (with Sections and Sentences added)
 * and Stanford's annotations via an AgigaDocument,
 * add these annotations and return a new Communication
 */
public class AgigaConcreteAnnotator {

    private boolean debug = false;
    private long timestamp;

    public AgigaConcreteAnnotator(){
    }
    public AgigaConcreteAnnotator(boolean debug){
        this.debug=debug;
    }


    private AnnotationMetadata metadata() {
        return new AnnotationMetadata.setTool("concrete-stanford").setTimestamp(timestamp);
    }
		
    private Communication comm;
    private UUID sectionSegmentationId;
    private List<UUID> sectionIds;
    private AgigaDocument agigaDoc;
    private int agigaSentPtr = -1;
    private int sectionPtr = -1;
    //number of sentences, indexed by section
    private int[] nummberOfSentences;
    // need to reference this in building corefs
    private List<Tokenization> tokenizations;

    public static String uuidStr(UUID id) {
        return id == null ? "null" : new java.util.UUID(id.getHigh(), id.getLow()).toString();
    }

    public synchronized void annotate(Communication comm,
                                      UUID sectionSegmentationId,
                                      List<UUID> sectionIds, // relevant sections (look inside for #sentences)
                                      int[] numOfSents,
                                      AgigaDocument agigaDoc) {

        if(sectionIds.size() == 0) {
            System.err.println("WARNING: calling annotate with no sections specified!");
            return comm.toBuilder().build();
        }
        if(debug){
            System.err.println("[AgigaConcreteAnnotator debug]");
            System.err.println("sectionSegmentationId = " + uuidStr(sectionSegmentationId));
        }
        this.numberOfSentences = numOfSents;
        this.timestamp = Calendar.getInstance().getTimeInMillis() / 1000;
        this.sectionSegmentationId = sectionSegmentationId;
        this.sectionIds = sectionIds;
        this.agigaDoc = agigaDoc;
        this.agigaSentPtr = 0;
        this.sectionPtr = 0;
        this.tokenizations = new ArrayList<Tokenization>();
        return f1(comm, false);
    }
	
    // Communication
    // SectionSegmentation
    // Section
    // SentenceSegmentation
    // Sentence
    // Tokenization

    // get appropriate section segmentation, and change it
    private void f1(Communication in, boolean transferCorefs) {
        SectionSegmentation newSS = null;
        int remove = -1;
        int n = in.getSectionSegmentationCount();
        for(int i=0; i<n; i++) {
            SectionSegmentation ss = in.getSectionSegmentation(i);
            if(ss.getUuid().equals(this.sectionSegmentationId)) {
                remove = i;
                f2(in, ss);
            }
            break;
        }
        if(remove < 0) throw new RuntimeException("couldn't find SectionSegmentation with UUID=" + this.sectionSegmentationId);
        if(this.tokenizations.size() != this.agigaDoc.getSents().size()) {
            throw new RuntimeException("#agigaSents=" + agigaDoc.getSents().size() + ", #tokenizations=" + tokenizations.size());
        }
    }

    public void addCorefs(Communication in){
        EntityMentionSet ems = new EntityMentionSet().setUuid(IdUtil.generateUUID()).setMetadata(metadata());
        EntitySet esb = new EntitySet().setUuid(IdUtil.generateUUID()).setMetadata(metadata());
        for(AgigaCoref coref : this.agigaDoc.getCorefs()) {
            Entity e = AgigaConverter.convertCoref(emsb, coref, this.agigaDoc, this.tokenizations);
            esb.addEntity(e);
        }
        in.addEntityMentionSet(emsb);
        in.addEntitySet(esb);
    }

    // add sections to the given section segmentation
    private void f2(SectionSegmentation in) {
        int n = in.getSectionCount();
        assert n > 0 : "n="+n;

        // add them from source
        UUID target = this.sectionIds.get(this.sectionPtr);
        if(debug)
            System.err.println("[f2] target=" + uuidStr(target));
        for(Section section : in.getSectionList()) {
            if(debug)
                System.err.printf("sectionPtr=%d sect.uuid=%s\n", sectionPtr, uuidStr(section.getUuid()));
            if(section.getUuid().equals(target)) {
                f3(section);
                this.sectionPtr++;
                if(debug) {
                    target = this.sectionPtr < this.sectionIds.size()
                        ? this.sectionIds.get(this.sectionPtr)
                        : null;
                    System.err.println("[f2] target=" + uuidStr(target));
                }
                break;
            }
        }
        if(this.sectionPtr != this.sectionIds.size())
            throw new RuntimeException(String.format("found %d of %d sections", this.sectionPtr, this.sectionIds.size()));
    }

    // add SentenceSegmentation
    private void f3(Section in) {
        if(debug)
            System.err.println("f3");
        //create a sentence segmentation
        SentenceSegmentation ss = new SentenceSegmentation().setUuid(IdUtils.???).setMetadata(metadata());
        UUID target = IdUtils.???;
        f4(ss);
        in.addSentenceSegmentation(ss);
    }

    // add all Sentences
    private void f4(SentenceSegmentation in) {
        if(debug) System.err.println("f4");
        int n = numberOfSentences[sectionPtr];
        assert n > 0 : "n=" + n;
        for(int i = 0; i < n; i++) in.addSentence(f5(s));
    }

    // add a Tokenization
    private Sentence f5(int index) {
        if(debug) System.err.println("f5");
        AgigaSentence asent = this.agigaDoc.getSents().get(agigaSentPtr++);
        Tokenization tok = AgigaConverter.convertTokenization(asent);	// tokenization has all the annotations
        this.tokenizations.add(tok);
        Sentence newS = new Sentence().;
        newS.addTokenization(tok);
        return newS;
    }
}

