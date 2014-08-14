package org.eclipse.tesla.plugins.proviso;

import junit.framework.TestCase;

public class XmlMarshallerTest extends TestCase {
  
  public void testXmlMarshaller() throws Exception {
    
    /*
    
    InputStream is = XmlMarshaller.class.getResourceAsStream("/assembly.xml");
    Reader reader = ReaderFactory.newXmlReader(is);
    Xpp3Dom dom = Xpp3DomBuilder.build(reader);
    Marshaller<ProvisioModel> m = new XmlMarshaller<ProvisioModel>();
    ProvisioModel assembly = new ProvisioModel();
    m.unmarshall(assembly, dom.getChild("runtimeAssembly"));

    List<ArtifactSet> fileSets = assembly.getFileSets();
    assertEquals(2, assembly.getFileSets().size());

    ArtifactSet lib = fileSets.get(0);
    assertEquals("lib", lib.getDirectory());
    List<ProvisoArtifact> libArtifacts = new ArrayList<ProvisoArtifact>(lib.getArtifactMapKeyedByGA().values());
    assertEquals("org.apache.maven", libArtifacts.get(0).getGroupId());
    assertEquals("maven-embedder", libArtifacts.get(0).getArtifactId());
    List<String> libExcludes = lib.getExcludes();
    assertEquals("org.codehaus.plexus:plexus-classworlds", libExcludes.get(0));

    List<ArtifactSet> libFileSets = lib.getFileSets();
    assertEquals(2, libFileSets.size());

    ArtifactSet mvnsh = libFileSets.get(0);
    assertEquals("mvnsh", mvnsh.getDirectory());
    List<ProvisoArtifact> mvnshArtifacts = new ArrayList<ProvisoArtifact>(mvnsh.getArtifactMapKeyedByGA().values());
    assertEquals(9,mvnshArtifacts.size());
    assertEquals(5,mvnsh.getExcludes().size());

    ArtifactSet ext = libFileSets.get(1);
    assertEquals("ext", ext.getDirectory());
    List<ProvisoArtifact> extArtifacts = new ArrayList<ProvisoArtifact>(ext.getArtifactMapKeyedByGA().values());
    assertEquals(6,extArtifacts.size());
    assertEquals(1,ext.getExcludes().size());
    
    ArtifactSet boot = fileSets.get(1);
    assertEquals("boot", boot.getDirectory());
    List<ProvisoArtifact> bootArtifacts = new ArrayList<ProvisoArtifact>(boot.getArtifactMapKeyedByGA().values());
    assertEquals("org.codehaus.plexus", bootArtifacts.get(0).getGroupId());
    assertEquals("plexus-classworlds", bootArtifacts.get(0).getArtifactId());

    */
  }
}
