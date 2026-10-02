<!--
  ~ Copyright (c) 1999-2026. Allette Systems Pty Ltd
  ~
  ~ Generate content.opf (ePub 3 Package Document) from the root PSML document.
  ~
  ~ OPF sections: metadata (Dublin Core + ePub) / manifest / spine.
  ~ Metadata driven by config/metadata in the export configuration.
  ~ Spine driven by <toc-tree>/<toc-part>, followed by other XHTML files as non-linear.
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:opf="http://www.idpf.org/2007/opf"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:m="http://www.w3.org/1998/Math/MathML"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:psf="http://www.pageseeder.com/function"
                xpath-default-namespace=""
                exclude-result-prefixes="xsl xs m psf">

  <xsl:include href="config.xsl"/>

  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <!-- URL of the folder containing the XHTML files -->
  <xsl:param name="xhtml-folder" />
  <!-- Comma-separated list of image filenames -->
  <xsl:param name="image-list" select="''"/>
  <!-- Comma-separated list of all generated XHTML basenames -->
  <xsl:param name="xhtml-list" select="''"/>

  <xsl:template match="/document">

    <xsl:variable name="doc" select="/"/>

    <!-- Scalars from the configuration -->
    <xsl:variable name="identifier" select="psf:identifier($doc)"/>
    <xsl:variable name="isbn-valid" select="starts-with($identifier, 'urn:isbn:')"/>
    <xsl:variable name="cfg-title"  select="psf:dc('title', $doc)"/>
    <xsl:variable name="creator"    select="psf:config-values($config/metadata/dc[@name='creator'][1], $doc)"/>
    <xsl:variable name="language"   select="(psf:dc('language', $doc)[. != ''], 'en')[1]"/>
    <xsl:variable name="publisher"  select="psf:dc('publisher', $doc)"/>
    <xsl:variable name="rights"     select="psf:dc('rights', $doc)"/>
    <xsl:variable name="cfg-date"   select="psf:dc('date', $doc)"/>
    <xsl:variable name="pub-subject" select="psf:dc('subject', $doc)"/>

    <!-- Cover: prefer the ANT-supplied flattened filename; else derive a stem
         from the xref href. Empty stem => no cover emitted. -->
    <xsl:variable name="cover-xref-href" select="//metadata//property[@name='cover_image']//image/@src"/>
    <xsl:variable name="cover-basename"
                  select="if ($cover-xref-href != '')
                            then replace(tokenize($cover-xref-href,'/')[last()],'\.[^.]+$','')
                          else ''"/>
    <xsl:variable name="has-cover" select="$cover-basename != ''"/>

    <!-- Unique component filenames -->
    <xsl:variable name="toc-files" as="xs:string*">
      <xsl:sequence select="tokenize(documentinfo/uri/@path, '/')[last()]"/>
      <xsl:for-each select="//toc-part[@href]">
        <xsl:sequence select="tokenize(@href, '/')[last()]"/>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="unique-files" select="distinct-values($toc-files)"/>
    <xsl:variable name="unique-images" select="tokenize($image-list, ',')[normalize-space(.)]"/>

    <!-- Generated XHTML files not in the TOC (non-linear) -->
    <xsl:variable name="toc-basenames" select="for $f in $unique-files return replace($f, '\.psml$', '')"/>
    <xsl:variable name="other-files"
                  select="tokenize($xhtml-list, ',')[normalize-space(.)][not(. = $toc-basenames)]"/>

    <!-- Title fallback chain (config title wins) -->
    <xsl:variable name="pub-title"
                  select="($cfg-title[. != ''],
                           documentinfo/uri/displaytitle,
                           'Untitled')[1]"/>

    <xsl:variable name="unique-items">
      <xsl:for-each select="$toc-basenames, $other-files">
        <xsl:variable name="basename" select="."/>
        <xsl:variable name="xhtml-file" select="document(concat(replace($xhtml-folder,'\\','/'),
                                 '/', $basename, '.xhtml'))" />
        <xsl:variable name="psdate" select="$xhtml-file//xhtml:meta[@name='pageseeder-date']/@content" />
        <item xmlns="http://www.idpf.org/2007/opf"
              id="xhtml-{$basename}" href="xhtml/{$basename}.xhtml" media-type="application/xhtml+xml">
          <xsl:if test="$xhtml-file//m:math">
            <xsl:attribute name="properties">mathml</xsl:attribute>
          </xsl:if>
          <xsl:if test="$psdate">
            <xsl:attribute name="date" select="$psdate"/>
          </xsl:if>
        </item>
      </xsl:for-each>
    </xsl:variable>

    <!-- ePub 3 dcterms:modified in strict UTC -->
    <xsl:variable name="latest-date" select="max($unique-items/opf:item/@date/xs:dateTime(.))" />
    <!-- Adjust the extracted maximum date to UTC (0 hour offset) -->
    <xsl:variable name="modified-epub" select="adjust-dateTime-to-timezone($latest-date, xs:dayTimeDuration('PT0H'))" />

    <!-- Detect mathml across the publication -->
    <xsl:variable name="has-mathml"
                  select="$unique-items/opf:item[contains(@properties, 'mathml')]"/>

    <package xmlns="http://www.idpf.org/2007/opf"
             version="3.0"
             unique-identifier="pub-id"
             xml:lang="{$language}"
             prefix="a11y: http://www.idpf.org/epub/vocab/package/a11y/#">

      <!-- ================= METADATA ================= -->
      <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">

        <!-- Identifier: real ISBN when valid, else fallback. -->
        <dc:identifier id="pub-id"><xsl:value-of select="$identifier"/></dc:identifier>
        <xsl:if test="$isbn-valid">
          <meta refines="#pub-id" property="identifier-type" scheme="onix:codelist5">15</meta>
        </xsl:if>

        <dc:title><xsl:value-of select="$pub-title"/></dc:title>

        <!-- Creator holds one or more authors in file-as form
             ("Surname, Given"). One dc:creator per author; display flips the
             first comma, file-as keeps the original. -->
        <xsl:for-each select="$creator">
          <xsl:variable name="a" select="normalize-space(.)"/>
          <xsl:variable name="disp"
                        select="if (contains($a,','))
                                then concat(normalize-space(substring-after($a,',')),' ',
                                            normalize-space(substring-before($a,',')))
                                else $a"/>
          <xsl:variable name="cid" select="concat('creator-', position())"/>
          <dc:creator id="{$cid}"><xsl:value-of select="$disp"/></dc:creator>
          <meta refines="#{$cid}" property="file-as"><xsl:value-of select="$a"/></meta>
          <meta refines="#{$cid}" property="role" scheme="marc:relators">aut</meta>
        </xsl:for-each>

        <dc:language><xsl:value-of select="$language"/></dc:language>

        <!-- Required ePub 3 modified timestamp -->
        <meta property="dcterms:modified"><xsl:value-of select="$modified-epub"/></meta>

        <xsl:if test="$cfg-date != ''">
          <dc:date><xsl:value-of select="$cfg-date"/></dc:date>
        </xsl:if>

        <xsl:if test="$pub-subject != ''">
          <dc:subject><xsl:value-of select="$pub-subject"/></dc:subject>
        </xsl:if>

        <xsl:if test="$publisher != ''">
          <dc:publisher><xsl:value-of select="$publisher"/></dc:publisher>
        </xsl:if>

        <xsl:if test="$rights != ''">
          <dc:rights><xsl:value-of select="$rights"/></dc:rights>
        </xsl:if>

        <dc:source>pageseeder:<xsl:value-of select="@publicationid"/></dc:source>

        <!-- Rendition -->
        <meta property="rendition:layout">reflowable</meta>
        <meta property="rendition:orientation">auto</meta>

        <!-- ============ OTHER METADATA (config meta, e.g. accessibility) ============ -->
        <xsl:for-each select="$config/metadata/meta[@name]">
          <xsl:variable name="name" select="string(@name)"/>
          <xsl:variable name="values" select="psf:config-values(., $doc)"/>
          <xsl:for-each select="$values">
            <meta property="{$name}"><xsl:value-of select="."/></meta>
          </xsl:for-each>
          <!-- Add MathML feature when math present and not already declared. -->
          <xsl:if test="$name = 'schema:accessibilityFeature' and $has-mathml and not($values = 'MathML')">
            <meta property="schema:accessibilityFeature">MathML</meta>
          </xsl:if>
        </xsl:for-each>

        <!-- Cover pointer (EPUB2-legacy) — only when a cover is identified. -->
        <xsl:if test="$has-cover">
          <meta name="cover" content="img-cover"/>
        </xsl:if>

      </metadata>

      <!-- ================= MANIFEST ================= -->
      <manifest xmlns="http://www.idpf.org/2007/opf">
        <item id="nav"  href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
        <item id="ncx"  href="toc.ncx"   media-type="application/x-dtbncx+xml"/>
        <item id="style-main" href="styles/epub.css" media-type="text/css"/>

        <!-- Content XHTML (remove @date which is not supported)-->
        <xsl:for-each select="$unique-items/opf:item">
          <xsl:copy>
            <xsl:copy-of select="@*[name()!='date']" />
          </xsl:copy>
        </xsl:for-each>

        <!-- Images. Cover identified by exact stem match against $cover-basename. -->
        <xsl:for-each select="$unique-images">
          <xsl:variable name="filename" select="."/>
          <xsl:variable name="basename" select="replace($filename, '\.[^.]+$', '')"/>
          <xsl:variable name="ext" select="lower-case(replace($filename, '^.*\.', ''))"/>
          <xsl:variable name="is-cover" select="$has-cover and $basename = $cover-basename"/>
          <xsl:variable name="item-id">
            <xsl:choose>
              <xsl:when test="$is-cover">img-cover</xsl:when>
              <xsl:otherwise>img-<xsl:value-of select="replace($basename, '[^a-zA-Z0-9_-]', '-')"/></xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <item id="{$item-id}" href="media/{$filename}">
            <xsl:attribute name="media-type">
              <xsl:choose>
                <xsl:when test="$ext = 'png'">image/png</xsl:when>
                <xsl:when test="$ext = ('jpg', 'jpeg')">image/jpeg</xsl:when>
                <xsl:when test="$ext = 'svg'">image/svg+xml</xsl:when>
                <xsl:when test="$ext = 'webp'">image/webp</xsl:when>
                <xsl:when test="$ext = 'gif'">image/gif</xsl:when>
                <xsl:otherwise>application/octet-stream</xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
            <xsl:if test="$is-cover">
              <xsl:attribute name="properties">cover-image</xsl:attribute>
            </xsl:if>
          </item>
        </xsl:for-each>
      </manifest>

      <!-- ================= SPINE ================= -->
      <spine toc="ncx">
        <itemref idref="nav" linear="no"/>

        <xsl:for-each select="$toc-basenames">
          <itemref idref="xhtml-{.}" linear="yes"/>
        </xsl:for-each>

        <xsl:for-each select="$other-files">
          <itemref idref="xhtml-{.}" linear="no"/>
        </xsl:for-each>

      </spine>

    </package>
  </xsl:template>

</xsl:stylesheet>
