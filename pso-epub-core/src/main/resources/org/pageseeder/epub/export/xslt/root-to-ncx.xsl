<!--
  ~ Copyright (c) 1999-2026. Allette Systems Pty Ltd
  ~
  ~ Generate toc.ncx (ePub 2 Navigation Control file) as a fallback.
  ~ Most modern readers use nav.xhtml exclusively, but NCX remains required
  ~ for broader compatibility and referenced by spine toc="ncx".
  ~
  ~ NCX is flat-ordered with playOrder numbering across all navPoints.
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.daisy.org/z3986/2005/ncx/"
                xmlns:psf="http://www.pageseeder.com/function"
                xpath-default-namespace=""
                exclude-result-prefixes="xsl">

  <xsl:include href="config.xsl"/>

  <xsl:output method="xml"
              encoding="UTF-8"
              indent="yes"
              omit-xml-declaration="no"
              doctype-public="-//NISO//DTD ncx 2005-1//EN"
              doctype-system="http://www.daisy.org/z3986/2005/ncx-2005-1.dtd"/>

  <xsl:template match="/document">
    <xsl:variable name="pub-title"
                  select="documentinfo/uri/displaytitle"/>
    <xsl:variable name="pub-filename"
              select="replace(tokenize(documentinfo/uri/@path, '/')[last()], '\.psml$', '.xhtml')"/>

    <!-- dtb:uid MUST match the OPF dc:identifier: use the same function as
         root-to-opf.xsl so the two never drift. -->
    <xsl:variable name="uid" select="psf:identifier(/)"/>

    <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
      <head>
        <meta name="dtb:uid" content="{$uid}"/>
        <meta name="dtb:depth">
          <xsl:attribute name="content">
            <xsl:value-of select="max(for $tp in //toc-part return count($tp/ancestor-or-self::toc-part))"/>
          </xsl:attribute>
        </meta>
        <meta name="dtb:totalPageCount" content="0"/>
        <meta name="dtb:maxPageNumber" content="0"/>
      </head>

      <docTitle>
        <text><xsl:value-of select="$pub-title"/></text>
      </docTitle>

      <navMap>
        <navPoint id="navPoint-1" playOrder="1">
          <navLabel>
            <text><xsl:value-of select="$pub-title"/></text>
          </navLabel>
          <content src="xhtml/{$pub-filename}" />
        </navPoint>
        <xsl:apply-templates select="//toc-tree/toc-part" />
      </navMap>
    </ncx>
  </xsl:template>

  <xsl:template match="toc-part">
    <!-- playOrder is the position of this toc-part in a flat document-order list. -->
    <xsl:variable name="play-order"
                  select="count(preceding::toc-part) + count(ancestor::toc-part) + 2"/>
    <navPoint id="navPoint-{$play-order}" playOrder="{$play-order}">
      <navLabel>
        <text><xsl:value-of select="@title"/></text>
      </navLabel>
      <content src="{psf:toc-part-href(.)}" />
      <xsl:apply-templates select="toc-part" />
    </navPoint>
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
