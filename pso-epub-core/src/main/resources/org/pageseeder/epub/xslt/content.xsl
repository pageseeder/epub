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

<!-- Match the root -->
	<xsl:template match="xhtml:html" mode="content">
		<xsl:apply-templates select="xhtml:head" mode="content" />
		<xsl:apply-templates select="xhtml:body" mode="content" />
	</xsl:template>

<!-- Match the head element -->
	<xsl:template match="xhtml:head" mode="content">
	</xsl:template>

<!-- Match the body element and transform each div into sections. 
Any existing anchors will be seperate sections as well, for reference of any hrefs that point to them -->
	<xsl:template match="xhtml:body" mode="content">
		<xsl:for-each-group select="xhtml:*"
			group-starting-with="xhtml:div|xhtml:*[@id]|xhtml:p[xhtml:*[@id]]">
			<xsl:comment>
			 <xsl:value-of select="./name()"/>-<xsl:value-of select="./@id"/>
			</xsl:comment>
			<xsl:choose>
			  <xsl:when test=".[@id]">
          <section id="{concat('a-',(./@id)[1])}">
            <fragment id="{concat('a-',(./@id)[1])}">
              <xsl:apply-templates select="current-group()"
                mode="content" />
            </fragment>
          </section>
        </xsl:when>
				<xsl:when test="current-group()//xhtml:*[@id]">
					<section id="{concat('a-',(current-group()//xhtml:*/@id)[1])}">
						<fragment id="{concat('a-',(current-group()//xhtml:*/@id)[1])}">
							<xsl:apply-templates select="current-group()"
								mode="content" />
						</fragment>
					</section>
				</xsl:when>
				<xsl:otherwise>
					<section id="{position()}">
						<fragment id="{position()}">
							<xsl:apply-templates select="current-group()"
								mode="content" />
						</fragment>
					</section>
				</xsl:otherwise>
			</xsl:choose>

		</xsl:for-each-group>
	</xsl:template>

<!-- Breaks are transformed into breaks -->
	<xsl:template match="xhtml:br" mode="content">
		<br />
		<xsl:apply-templates mode="content" />
	</xsl:template>

<!-- Each div is transformed into a paraLabel with the class of the div set as the paraLabel name -->
	<xsl:template match="xhtml:div" mode="content">
		<block label="{replace(@class,'[^a-zA-Z0-9_-]','_')}">
			<xsl:for-each-group select="node()"
				group-starting-with="xhtml:br">
				<xsl:apply-templates select="current-group()"
					mode="content" />
			</xsl:for-each-group>
		</block>
	</xsl:template>

<!-- Headings -->
	<xsl:template match="xhtml:h1" mode="content">
		<heading level="1">
			<xsl:apply-templates mode="content" />
		</heading>
	</xsl:template>

	<xsl:template match="xhtml:h2" mode="content">
		<heading level="2">
			<xsl:apply-templates mode="content" />
		</heading>
	</xsl:template>

	<xsl:template match="xhtml:h3" mode="content">
		<heading level="3">
			<xsl:apply-templates mode="content" />
		</heading>
	</xsl:template>

	<xsl:template match="xhtml:h4" mode="content">
		<heading level="4">
			<xsl:apply-templates mode="content" />
		</heading>
	</xsl:template>

	<xsl:template match="xhtml:h5" mode="content">
		<heading level="5">
			<xsl:apply-templates mode="content" />
		</heading>
	</xsl:template>

	<xsl:template match="xhtml:h6" mode="content">
		<heading level="6">
			<xsl:apply-templates mode="content" />
		</heading>
	</xsl:template>

<!-- Span element is transformed into inlineLabel and the class is the name of the inlineLabel -->
	<xsl:template match="xhtml:span" mode="content">
		<inline label="{replace(@class,'[^a-zA-Z0-9_-]','_')}">
			<xsl:apply-templates mode="content" />
		</inline>
	</xsl:template>

<!-- element p is transformed into a para except when it has a class, in which case it is transformed into a paraLabel -->
	<xsl:template match="xhtml:p" mode="content">
		<para>
			<xsl:apply-templates mode="content" />
		</para>
	</xsl:template>

	<xsl:template match="xhtml:p[@class]" mode="content">
	 <xsl:choose>
     <xsl:when test="parent::xhtml:*[not(@class)]">
     <para>
        <inline label="{replace(@class,'[^a-zA-Z0-9_-]','_')}">
		      <xsl:apply-templates mode="content" />
		    </inline>
		 </para>   
     </xsl:when>
     <xsl:otherwise>
       <block label="{replace(@class,'[^a-zA-Z0-9_-]','_')}">
		      <xsl:apply-templates mode="content" />
		    </block>
     </xsl:otherwise>
  </xsl:choose>
		
	</xsl:template>

<!-- Element table is a table -->
	<xsl:template match="xhtml:table" mode="content">
		<table>
			<xsl:apply-templates mode="content" />
		</table>
	</xsl:template>

<!-- Element tr is a row -->
	<xsl:template match="xhtml:tr" mode="content">
		<row>
			<xsl:apply-templates mode="content" />
		</row>
	</xsl:template>

<!-- element td is a cell; if there is an element div inside of the cell, the element is transformed into a paraLabel ( see match="xhtml:div"); -->
<!-- otherwise all content is placed inside of a para -->
	<xsl:template match="xhtml:td" mode="content">
		<cell>
			<xsl:for-each select="node()">
				<xsl:choose>
					<xsl:when test="./name() = 'div'">
						<xsl:apply-templates mode="content" select="." />
					</xsl:when>
					<xsl:otherwise>
						<para>
							<xsl:apply-templates mode="content" select="." />
						</para>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		</cell>
	</xsl:template>

<!-- Formatting -->
	<xsl:template match="xhtml:b" mode="content">
		<bold>
			<xsl:apply-templates mode="content" />
		</bold>
	</xsl:template>

	<xsl:template match="xhtml:i" mode="content">
		<italic>
			<xsl:apply-templates mode="content" />
		</italic>
	</xsl:template>

	<xsl:template match="xhtml:sub" mode="content">
		<sub>
			<xsl:apply-templates mode="content" />
		</sub>
	</xsl:template>

	<xsl:template match="xhtml:sup" mode="content">
		<sup>
			<xsl:apply-templates mode="content" />
		</sup>
	</xsl:template>

	<xsl:template match="xhtml:u" mode="content">
		<underline>
			<xsl:apply-templates mode="content" />
		</underline>
	</xsl:template>

  <xsl:template match="xhtml:cite" mode="content">
    <inline label="cite">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>

  <xsl:template match="xhtml:em" mode="content">
    <inline label="em">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>
  
  <xsl:template match="xhtml:var" mode="content">
    <inline label="var">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>  

  <xsl:template match="xhtml:samp" mode="content">
    <inline label="samp">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>   
  
  <xsl:template match="xhtml:small" mode="content">
    <inline label="small">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>   
          
  <xsl:template match="xhtml:strong" mode="content">
    <inline label="strong">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>   
  
  <xsl:template match="xhtml:mark" mode="content">
    <inline label="mark">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>   
  
  <xsl:template match="xhtml:ins" mode="content">
    <inline label="ins">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>   
  
  <xsl:template match="xhtml:del" mode="content">
    <inline label="del">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>   
  
  <xsl:template match="xhtml:s" mode="content">
    <inline label="s">
      <xsl:apply-templates mode="content" />
    </inline>
  </xsl:template>   
  
  <xsl:template match="xhtml:code" mode="content">
    <code>
      <xsl:apply-templates mode="content" />
    </code>
  </xsl:template>   
  
          
<!-- Lists -->
	<xsl:template match="xhtml:ul" mode="content">
		<list>
			<xsl:apply-templates mode="content" />
		</list>
	</xsl:template>

	<xsl:template match="xhtml:ol" mode="content">
		<nlist>
			<xsl:apply-templates mode="content" />
		</nlist>
	</xsl:template>

	<xsl:template match="xhtml:li" mode="content">
		<item>
			<xsl:apply-templates mode="content" />
		</item>
	</xsl:template>

	<xsl:template match="text()" mode="content">
		<xsl:copy-of select="." />
	</xsl:template>

<!-- Element img is transformed into graphic. if it has a class, a paraLabel is created with the image inside-->
	<xsl:template match="xhtml:img" mode="content">
		<image>
			<xsl:if test="@width and @width != ''">
				<xsl:attribute name="width" select="@width" />
			</xsl:if>
			<xsl:if test="@height and @height != ''">
				<xsl:attribute name="height" select="@height" />
			</xsl:if>
			<xsl:attribute name="src">
        <xsl:value-of select="concat('../media/',@src)" />
      </xsl:attribute>
		</image>
	</xsl:template>

	<xsl:template match="xhtml:img[@class]" mode="content">
	<xsl:choose>
	   <xsl:when test="parent::xhtml:*[not(@class)]">
	     <inline label="{@class}">
          <image>
            <xsl:if test="@width and @width != ''">
              <xsl:attribute name="width" select="@width" />
            </xsl:if>
            <xsl:if test="@height and @height != ''">
              <xsl:attribute name="height" select="@height" />
            </xsl:if>
            <xsl:attribute name="src">
              <xsl:value-of select="concat('../media/',@src)" />
            </xsl:attribute>
          </image>
        </inline>
	   </xsl:when>
	   <xsl:otherwise>
	     <block label="{@class}">
		      <image>
		        <xsl:if test="@width and @width != ''">
		          <xsl:attribute name="width" select="@width" />
		        </xsl:if>
		        <xsl:if test="@height and @height != ''">
		          <xsl:attribute name="height" select="@height" />
		        </xsl:if>
		        <xsl:attribute name="src">
		          <xsl:value-of select="concat('../media/',@src)" />
		        </xsl:attribute>
		      </image>
        </block>
	   </xsl:otherwise>
	</xsl:choose>
		
	</xsl:template>
  

<!-- element a is transformed into link; if the link contains an href, it is a reference to another document;  -->
<!-- if it does not contain an href, then it is an anchor  -->
	<xsl:template match="xhtml:a[@id]" mode="content">
		<link>
			<xsl:attribute name="name">
			 <xsl:value-of select="@id" />
		  </xsl:attribute>
		</link>
	</xsl:template>

	<xsl:template match="xhtml:a[fn:isLocal(@href) = true()][@href]"
		mode="content">
		<xsl:param name="file-names" tunnel="yes" as="element()" />
		<xsl:variable name="simple-href">
		  <xsl:choose>
		    <xsl:when test="contains(@href,'#')">
		      <xsl:value-of select="substring-before(@href,'#')" />
		    </xsl:when>
		    <xsl:otherwise>
		      <xsl:value-of select="@href" />
		    </xsl:otherwise>
		  </xsl:choose>
			
		</xsl:variable>
		<xsl:message><xsl:value-of select="$simple-href" /></xsl:message>
		<xref display="manual"
			type="none" reverselink="true" reversetitle=""
			reversetype="none" href="{$file-names//file[@original = $simple-href]/@new}">
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
			<xsl:attribute name="frag">
       <xsl:choose>
         <xsl:when test="contains(@href,'#')">
           <xsl:value-of select="concat('a-',substring-after(@href,'#'))" />
         </xsl:when>
         <xsl:otherwise>
           <xsl:value-of select="'default'" />
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
		</xref>
	</xsl:template>

	<xsl:template match="xhtml:a[fn:isLocal(@href) = false()][@href]"
		mode="content">
		<link href="{@href}">
			<xsl:choose>
				<xsl:when test="@title">
					<xsl:value-of select="@title" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="fn:string-before-last-delimiter(@href,'\.')" />
				</xsl:otherwise>
			</xsl:choose>
		</link>
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
