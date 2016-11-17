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

<!--
  Main document for transformations from epub to PSML/psxml formats
-->

<xsl:variable name="inline">
    <xsl:for-each select="$configDoc/config/inline/element/@psml">
      <xsl:choose>
        <xsl:when test="position() = last()">
          <xsl:value-of select="." />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat(.,'|')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:variable>
  
  <xsl:variable name="block">
    <xsl:for-each select="$configDoc/config/block/element/@psml">
      <xsl:choose>
        <xsl:when test="position() = last()">
          <xsl:value-of select="." />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat(.,'|')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:variable>
  
  <xsl:variable name="table">
    <xsl:for-each select="$configDoc/config/table/element/@psml">
      <xsl:choose>
        <xsl:when test="position() = last()">
          <xsl:value-of select="." />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat(.,'|')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:variable>
  
  <xsl:variable name="lists">
    <xsl:for-each select="$configDoc/config/lists/element/@psml">
      <xsl:choose>
        <xsl:when test="position() = last()">
          <xsl:value-of select="." />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat(.,'|')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:variable>
  
<!-- Match the root -->
<xsl:template natch="/" mode="content">

</xsl:template>



  <xsl:template match="document" mode="content">
    <xsl:choose>
      <xsl:when test="not(ancestor::document)">
        <xhtml:html>
          <xhtml:head/>
          <xhtml:body>
            <xsl:apply-templates mode="content" />
          </xhtml:body>
        </xhtml:html>
      </xsl:when>
      <xsl:when test="$configDoc/config/split">
        <xhtml:html>
          <xhtml:head/>
          <xhtml:body>
            <xsl:apply-templates mode="content" />
          </xhtml:body>
        </xhtml:html>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates mode="content" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!--##properties##-->
  <xsl:template match="xref-fragment" mode="content">
    <xsl:apply-templates mode="content" />
  </xsl:template>
  
  <!--##section##-->
  <xsl:template match="section" mode="content">
    <xhtml:div>
      <xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
      <xsl:apply-templates mode="content" />
    </xhtml:div>
  </xsl:template>
  
  <xsl:template match="fragment" mode="content">
    <xsl:apply-templates mode="content" />
  </xsl:template>
 
<!-- Breaks are transformed into breaks -->
	<xsl:template match="br" mode="content">
		<xhtml:br />
		<xsl:apply-templates mode="content" />
	</xsl:template>

<!-- Each div is transformed into a paraLabel with the class of the div set as the paraLabel name -->
	<xsl:template match="block" mode="content">
		<xhtml:div class="{@label}">
				<xsl:apply-templates mode="content" />
		</xhtml:div>
	</xsl:template>

<!-- Headings -->
<!--   <xsl:template match="heading[@level = '1']" mode="content"> -->
<!--     <xhtml:h1> -->
<!--       <xsl:apply-templates mode="content" /> -->
<!--     </xhtml:h1> -->
<!--   </xsl:template> -->
  
<!--   <xsl:template match="heading[@level = '2']" mode="content"> -->
<!--     <xhtml:h2> -->
<!--       <xsl:apply-templates mode="content" /> -->
<!--     </xhtml:h2> -->
<!--   </xsl:template> -->
  
<!--   <xsl:template match="heading[@level = '3']" mode="content"> -->
<!--     <xhtml:h3> -->
<!--       <xsl:apply-templates mode="content" /> -->
<!--     </xhtml:h3> -->
<!--   </xsl:template> -->
  
<!--   <xsl:template match="heading[@level = '4']" mode="content"> -->
<!--     <xhtml:h4> -->
<!--       <xsl:apply-templates mode="content" /> -->
<!--     </xhtml:h4> -->
<!--   </xsl:template> -->
  
<!--   <xsl:template match="heading[@level = '5']" mode="content"> -->
<!--     <xhtml:h5> -->
<!--       <xsl:apply-templates mode="content" /> -->
<!--     </xhtml:h5> -->
<!--   </xsl:template> -->
  
<!--   <xsl:template match="heading[@level = '6']" mode="content"> -->
<!--     <xhtml:h6> -->
<!--       <xsl:apply-templates mode="content" /> -->
<!--     </xhtml:h6> -->
<!--   </xsl:template> -->

<!-- Span element is transformed into inlineLabel and the class is the name of the inlineLabel -->
	<xsl:template match="inline" mode="content">
		<xhtml:span class="{@label}">
			<xsl:apply-templates mode="content" />
		</xhtml:span>
	</xsl:template>

<!-- element p is transformed into a para except when it has a class, in which case it is transformed into a paraLabel -->
	<xsl:template match="xhtml:para" mode="content">
		<p>
			<xsl:apply-templates mode="content" />
		</p>
	</xsl:template>

<!-- Element table is a table -->
	<xsl:template match="table" mode="content">
		<xhtml:table>
			<xsl:apply-templates mode="content" />
		</xhtml:table>
	</xsl:template>

<!-- Element tr is a row -->
	<xsl:template match="row" mode="content">
		<xhtml:tr>
			<xsl:apply-templates mode="content" />
		</xhtml:tr>
	</xsl:template>

<!-- element td is a cell; if there is an element div inside of the cell, the element is transformed into a paraLabel ( see match="xhtml:div"); -->
<!-- otherwise all content is placed inside of a para -->
	<xsl:template match="cell" mode="content">
		<xhtml:td>
			<xsl:apply-templates mode="content" select="." />
		</xhtml:td>
	</xsl:template>

<!-- Formatting -->
<!-- 	<xsl:template match="bold" mode="content"> -->
<!-- 		<xhtml:b> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:b> -->
<!-- 	</xsl:template> -->

<!-- 	<xsl:template match="italic" mode="content"> -->
<!-- 		<xhtml:i> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:i> -->
<!-- 	</xsl:template> -->

<!-- 	<xsl:template match="sub" mode="content"> -->
<!-- 		<xhtml:sub> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:sub> -->
<!-- 	</xsl:template> -->

<!-- 	<xsl:template match="sup" mode="content"> -->
<!-- 		<xhtml:sup> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:sup> -->
<!-- 	</xsl:template> -->

<!-- 	<xsl:template match="underline" mode="content"> -->
<!-- 		<xhtml:u> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:u> -->
<!-- 	</xsl:template> -->
  
<!--   <xsl:template match="code" mode="content"> -->
<!--     <xhtml:code> -->
<!--       <xsl:apply-templates mode="content" /> -->
<!--     </xhtml:code> -->
<!--   </xsl:template>    -->
  
<!-- Lists -->
<!-- 	<xsl:template match="list" mode="content"> -->
<!-- 		<xhtml:ul> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:ul> -->
<!-- 	</xsl:template> -->

<!-- 	<xsl:template match="nlist" mode="content"> -->
<!-- 		<xhtml:ol> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:ol> -->
<!-- 	</xsl:template> -->

<!-- 	<xsl:template match="item" mode="content"> -->
<!-- 		<xhtml:li> -->
<!-- 			<xsl:apply-templates mode="content" /> -->
<!-- 		</xhtml:li> -->
<!-- 	</xsl:template> -->

	<xsl:template match="text()" mode="content">
		<xsl:copy-of select="." />
	</xsl:template>

<!-- Element img is transformed into graphic. if it has a class, a paraLabel is created with the image inside-->
	<xsl:template match="image" mode="content">
		<xhtml:img>
			<xsl:if test="@width and @width != ''">
				<xsl:attribute name="width" select="@width" />
			</xsl:if>
			<xsl:if test="@height and @height != ''">
				<xsl:attribute name="height" select="@height" />
			</xsl:if>
			<xsl:attribute name="src" select="@src" />
		</xhtml:img>
	</xsl:template>

	
<!-- element a is transformed into link; if the link contains an href, it is a reference to another document;  -->
<!-- if it does not contain an href, then it is an anchor  -->
	<xsl:template match="link" mode="content">
		<xhtml:a>
			<xsl:attribute name="id" select="@name" />
		</xhtml:a>
	</xsl:template>
	
	<xsl:template match="xref" mode="content">
	 <xhtml:a>
	   <xsl:attribute name="title">
       <xsl:choose>
         <xsl:when test="@title">
           <xsl:value-of select="@title" />
         </xsl:when>
         <xsl:otherwise>
           <xsl:value-of select="." />
         </xsl:otherwise>
       </xsl:choose>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="@title">
          <xsl:value-of select="@title" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="." />
        </xsl:otherwise>
      </xsl:choose>
	 </xhtml:a>
	</xsl:template>

  <xsl:template match="*" mode="content">
  <xsl:choose>
    <xsl:when test="matches(*/name(),$block)">
      <xsl:choose>
        <xsl:when test="$configDoc/config/block/element[@psml='name()'][@xhtml='div']">
          <xsl:element name="div">
            <xsl:attribute name="class"><xsl:value-of select="$configDoc/config/block/element[@psml='name()']/@xhtmlvalue"/></xsl:attribute>
            <xsl:apply-templates mode="content" select="." />
          </xsl:element>
        </xsl:when>
        <xsl:when test="$configDoc/config/block/element[@psml='name()'][@xhtml='span']">
          <xsl:element name="span">
            <xsl:attribute name="class"><xsl:value-of select="$configDoc/config/block/element[@psml='name()']/@xhtmlvalue"/></xsl:attribute>
            <xsl:apply-templates mode="content" select="." />
          </xsl:element>
        </xsl:when>
        <xsl:when test="$configDoc/config/block/element[@psml='name()'][@xhtmlvalue!='']">
          <xsl:element name="{$configDoc/config/block/element[@xhtml='name()']/@xhtml}">
            <xsl:attribute name="class"><xsl:value-of select="$configDoc/config/block/element[@psml='name()']/@xhtmlvalue"/></xsl:attribute>
            <xsl:apply-templates mode="content" select="." />
          </xsl:element>
        </xsl:when>
        <xsl:otherwise>
          <xsl:element name="{$configDoc/config/block/element[@psml='name()']/@xhtml}">
            <xsl:apply-templates mode="content" select="." />
          </xsl:element>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="matches(*/name(),$inline)">
      <xsl:choose>
        <xsl:when test="$configDoc/config/inline/element[@psml='name()'][@xhtml='div']">
          <xsl:element name="div">
            <xsl:attribute name="class"><xsl:value-of select="$configDoc/config/inline/element[@psml='name()']/@xhtmlvalue"/></xsl:attribute>
            <xsl:apply-templates mode="content" select="." />
          </xsl:element>
        </xsl:when>
        <xsl:when test="$configDoc/config/inline/element[@psml='name()'][@xhtml='span']">
          <xsl:element name="span">
            <xsl:attribute name="class"><xsl:value-of select="$configDoc/config/inline/element[@psml='name()']/@xhtmlvalue"/></xsl:attribute>
            <xsl:apply-templates mode="content" select="." />
          </xsl:element>
        </xsl:when>
        <xsl:otherwise>
          <xsl:element name="{$configDoc/config/inline/element[@psml='name()']/@psml}">
            <xsl:apply-templates mode="content" select="." />
          </xsl:element>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="matches(*/name(),$table)">
      <xsl:element name="{$configDoc/config/table/element[@psml='name()']/@xhtml}">
        <xsl:apply-templates mode="content" select="." />
      </xsl:element>
    </xsl:when>
    <xsl:when test="matches(*/name(),$lists)">
      <xsl:element name="{$configDoc/config/lists/element[@psml='name()']/@xhtml}">
        <xsl:apply-templates mode="content" select="." />
      </xsl:element>
    </xsl:when>
    <xsl:otherwise>
        <xsl:apply-templates mode="content" select="." />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

  
	<xsl:function name="fn:isLocal">
		<xsl:param name="href" />
		<xsl:choose>
			<xsl:when test="starts-with($href,'http://')">
				<xsl:value-of select="false()" />
			</xsl:when>
			<xsl:when test="starts-with($href,'https://')">
        <xsl:value-of select="false()" />
      </xsl:when>
			<xsl:when test="$href=''">
				<xsl:value-of select="false()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="true()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<xsl:function name="fn:string-before-last-delimiter">
		<xsl:param name="string" />
		<xsl:param name="delimiter" />
		<xsl:analyze-string regex="^(.*)[{$delimiter}][^{$delimiter}]+"
			select="$string">
			<xsl:matching-substring>
				<xsl:value-of select="regex-group(1)" />
			</xsl:matching-substring>
		</xsl:analyze-string>
	</xsl:function>


</xsl:stylesheet>
