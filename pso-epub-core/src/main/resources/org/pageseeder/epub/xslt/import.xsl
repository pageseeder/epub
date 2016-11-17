<!--
  This is the default template to import an EPUB as PSML.

  @author Christophe Lauret
  @version 18 February 2013
-->
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:root="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:opf="http://www.idpf.org/2007/opf"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:fn="http://www.pageseeder.com/function" exclude-result-prefixes="#all">

	<xsl:include href="import/content.xsl" />

<!-- Root folder -->
	<xsl:param name="_rootfolder" />

<!-- Output folder -->
	<xsl:param name="_outputfolder" />

<!-- epub file name [file_name].epub -->
	<xsl:param name="_epubfilename" />
  
  <xsl:variable name="configDoc" select="document('import/wpml-config.xml')" />
<!-- CONTAINER.XML ============================================================================ -->

<!--
  The document element of the EPUB META-INF/container.xml
  For each root file available run the script
-->
	<xsl:template match="root:container">
		<xsl:for-each select="descendant::root:rootfile">
			<xsl:variable name="context_folder">
				<xsl:value-of select="substring-before(@full-path,'/')" />
			</xsl:variable>
    <!-- Find each root file, the full path starts from the EPUB / -->
			<xsl:apply-templates select="document(concat($_rootfolder, @full-path))">
				<xsl:with-param name="context_folder" tunnel="yes">
					<xsl:value-of select="$context_folder" />
				</xsl:with-param>
			</xsl:apply-templates>
		</xsl:for-each>
	</xsl:template>

<!-- CONTENT.OPF ============================================================================== -->

<!--
  Package information from the 'content.opf' file
-->
	<xsl:template match="opf:package">
  <!-- TODO! -->

		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="opf:metadata">
	</xsl:template>


	<xsl:template match="opf:manifest">

	</xsl:template>

<!-- The spine element contains the list of referenced files inside the epub
Each of these files will be used to generate psml files and reference them inside the master psml file -->
	<xsl:template match="opf:spine">
		<xsl:param name="context_folder" tunnel="yes" />
		<document level="portable">
			<xsl:variable name="base-folder">
				<xsl:value-of
					select="replace(substring-before($_epubfilename,'.epub'),'[^a-zA-Z0-9_-]','_')" />
			</xsl:variable>
      
      <!-- This variable is used to map the file names (from the referenced files by each itemref to the generated files) -->
			<xsl:variable name="file-names" as="element()">
				<files>
					<xsl:for-each select="opf:itemref">
						<xsl:variable name="idref">
							<xsl:value-of select="@idref" />
						</xsl:variable>
						<file>
							<xsl:attribute name="original">
                <xsl:value-of
								select="../../opf:manifest/opf:item[@id=$idref]/@href" />
              </xsl:attribute>
							<xsl:attribute name="new">
                <xsl:value-of
								select="concat($base-folder,'-',position(),'.xml')" />
              </xsl:attribute>
						</file>
					</xsl:for-each>
				</files>
			</xsl:variable>
      
      <xsl:variable name="number-of-files">
        <xsl:value-of select="count(opf:itemref)"/>
      </xsl:variable>
      
       <xsl:variable name="zeropadding">
      <xsl:choose>
        <xsl:when test="$number-of-files &lt; 10">
          <xsl:value-of select="'0'" />
        </xsl:when>
        <xsl:when test="$number-of-files &lt; 100">
          <xsl:value-of select="'00'" />
        </xsl:when>
        <xsl:when test="$number-of-files &lt; 1000">
          <xsl:value-of select="'000'" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'0000'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
			<section id="content">
				<fragment format="psxreflist" id="content">
					<xsl:value-of select="$file-names" />
					<xsl:for-each select="opf:itemref">

						<xsl:variable name="file-name">
							<xsl:value-of select="concat($base-folder,'-',format-number(position(), $zeropadding),'.psml')"></xsl:value-of>
						</xsl:variable>
						<xsl:variable name="idref">
							<xsl:value-of select="@idref" />
						</xsl:variable>

						<xsl:variable name="href-file">
							<xsl:value-of select="../../opf:manifest/opf:item[@id=$idref]/@href" />
						</xsl:variable>
            <xsl:message><xsl:value-of select="$href-file" /> -> <xsl:value-of select="$file-name" /></xsl:message>
						<xsl:result-document
							href="{concat($_outputfolder,$base-folder,'/',$file-name)}">
<!-- 							<xsl:processing-instruction name="stylesheet"> -->
<!-- 								<xsl:text>format="standard"</xsl:text> -->
<!-- 							</xsl:processing-instruction> -->
							<document level="portable">
								<xsl:variable name="doc"
									select="document(concat($_rootfolder, $context_folder, '/', $href-file))" />
								<xsl:apply-templates select="$doc" mode="content">
									<xsl:with-param name="file-names" tunnel="yes"
										as="element()" select="$file-names" />
								</xsl:apply-templates>
							</document>
						</xsl:result-document>
                
						<blockxref title=""
							frag="default" display="manual" type="embed"
							reverselink="true" reversetitle="" reversetype="none"
							href="{concat($base-folder,'/',encode-for-uri($file-name))}">
							<xsl:value-of select="substring-before($file-name,'.xml')" />
						</blockxref>
					</xsl:for-each>

				</fragment>
			</section>
		</document>
	</xsl:template>

	<xsl:template match="guide">
		<xsl:apply-templates />
	</xsl:template>

</xsl:stylesheet>
