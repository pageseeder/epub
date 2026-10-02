<!--
  ~ Copyright (c) 1999-2026. Allette Systems Pty Ltd
  ~
  ~ Generate nav.xhtml (ePub 3 Navigation Document) from the <toc-tree>/<toc-part> elements
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:epub="http://www.idpf.org/2007/ops"
                xmlns:psf="http://www.pageseeder.com/function"
                xpath-default-namespace=""
                exclude-result-prefixes="xsl">

  <xsl:include href="config.xsl"/>

  <xsl:output method="xml"
              encoding="UTF-8"
              indent="yes"
              omit-xml-declaration="no"/>

  <xsl:template match="/document">
    <xsl:variable name="pub-title"
                  select="documentinfo/uri/displaytitle"/>
    <xsl:variable name="pub-filename"
              select="replace(tokenize(documentinfo/uri/@path, '/')[last()], '\.psml$', '.xhtml')"/>
    <html xmlns="http://www.w3.org/1999/xhtml"
          xmlns:epub="http://www.idpf.org/2007/ops"
          xml:lang="en"
          lang="en">
      <head>
        <meta charset="UTF-8"/>
        <title><xsl:value-of select="$pub-title"/></title>
        <link rel="stylesheet" type="text/css" href="styles/epub.css"/>
      </head>
      <body>
        <!-- Primary TOC (required by ePub 3) -->
        <nav epub:type="toc" id="toc" role="doc-toc">
          <h1><xsl:value-of select="$pub-title"/></h1>
          <ol>
            <li>
              <a href="xhtml/{$pub-filename}"><xsl:value-of select="$pub-title"/></a>
            </li>            
            <xsl:apply-templates select="//toc-tree/toc-part"/>
          </ol>
        </nav>

        <!-- Landmarks (optional but Thorium-friendly) -->
        <nav epub:type="landmarks" id="landmarks" hidden="hidden">
          <h2>Landmarks</h2>
          <ol>
            <li>
              <a epub:type="toc" href="nav.xhtml">Table of Contents</a></li>
            <li>
              <a epub:type="bodymatter" href="{psf:toc-part-href((//toc-tree/toc-part)[1])}">Start</a>
            </li>
            <!-- Glossary and bibliography (only when configured): the first component in the
                 TOC whose document type is the configured one -->
            <xsl:if test="exists($glossary-type)">
              <xsl:call-template name="landmark">
                <xsl:with-param name="type" select="'glossary'"/>
                <xsl:with-param name="documenttype" select="$glossary-type"/>
              </xsl:call-template>
            </xsl:if>
            <xsl:if test="exists($bibliography-type)">
              <xsl:call-template name="landmark">
                <xsl:with-param name="type" select="'bibliography'"/>
                <xsl:with-param name="documenttype" select="$bibliography-type"/>
              </xsl:call-template>
            </xsl:if>
          </ol>
        </nav>
      </body>
    </html>
  </xsl:template>

  <!-- Landmark to the first component in the TOC of the specified document type (if any) -->
  <xsl:template name="landmark">
    <xsl:param name="type"/>
    <xsl:param name="documenttype"/>
    <xsl:variable name="part" select="(//toc-tree//toc-part[@href][document(@href, .)/document/@type = $documenttype])[1]"/>
    <xsl:if test="$part">
      <li>
        <a epub:type="{$type}" href="{psf:toc-part-href($part)}"><xsl:value-of select="$part/@title"/></a>
      </li>
    </xsl:if>
  </xsl:template>

  <!-- Recursive rendering of toc-part and its children -->
  <xsl:template match="toc-part">
    <li>
      <a href="{psf:toc-part-href(.)}">
        <xsl:value-of select="@title"/>
      </a>
      <xsl:if test="toc-part">
        <ol>
          <xsl:apply-templates select="toc-part"/>
        </ol>
      </xsl:if>
    </li>
  </xsl:template>

  <!-- Translate toc-part @href and @idref into an XHTML href.
       Output: "xhtml/filename.xhtml#[idref]" -->
  <xsl:function name="psf:toc-part-href">
    <xsl:param name="toc-part"/>
    <xsl:variable name="href"
                  select="if ($toc-part/@href) then $toc-part/@href else $toc-part/ancestor::toc-part[1]/@href" />
    <xsl:variable name="filename"
                  select="replace(tokenize($href, '/')[last()], '\.psml$', '.xhtml')"/>
    <xsl:value-of select="concat('xhtml/', $filename, '#', $toc-part/@idref)"/>
  </xsl:function>

</xsl:stylesheet>
