<!--
  ~ Copyright (c) 1999-2026. Allette Systems Pty Ltd
  ~
  ~ Transform processed PSML component documents into XHTML for ePub 3.3.
  ~
  ~ Output: one XHTML file per input PSML.
  ~ Filename convention: matches source (e.g. 001-lesson.psml → 001-lesson.xhtml)
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:epub="http://www.idpf.org/2007/ops"
                xmlns:m="http://www.w3.org/1998/Math/MathML"
                xmlns:psf="http://www.pageseeder.com/function"
                xpath-default-namespace=""
                exclude-result-prefixes="xsl psf">

  <xsl:include href="config.xsl"/>

  <xsl:output method="xml"
              encoding="UTF-8"
              indent="yes"
              omit-xml-declaration="no"
              doctype-system=""/>


  <!-- =====================================================================
       ROOT: one XHTML document per input PSML
       ===================================================================== -->
  <xsl:template match="/document">
    <html xmlns="http://www.w3.org/1999/xhtml"
          xmlns:epub="http://www.idpf.org/2007/ops"
          xml:lang="en"
          lang="en">
      <head>
        <meta charset="UTF-8"/>
        <title>
          <xsl:value-of select="(documentinfo/uri/displaytitle, documentinfo/uri/@title, 'Untitled')[1]"/>
        </title>
        <link rel="stylesheet" type="text/css" href="../styles/epub.css"/>
        <!-- Preserve publication metadata as HTML meta -->
        <meta name="pageseeder-uriid"      content="{@id}"/>
        <meta name="pageseeder-documenttype" content="{documentinfo/uri/@documenttype}"/>
        <xsl:if test="@publicationid">
          <meta name="pageseeder-publicationid" content="{@publicationid}"/>
        </xsl:if>
        <meta name="pageseeder-date" content="{@date}" />
      </head>
      <body>
        <xsl:attribute name="epub:type">bodymatter</xsl:attribute>
        <xsl:attribute name="class">
          <xsl:value-of select="string-join(
            (documentinfo/uri/@documenttype,
             tokenize(documentinfo/uri/labels, ',')[normalize-space()]), ' ')"/>
        </xsl:attribute>

        <article id="{@id}-default">
          <xsl:apply-templates select="section"/>
        </article>

        <!-- Footnote/endnote bodies inlined at end of THIS document so popup-capable
             readers (Apple Books, Thorium) render them as popups; others show
             the section inline as endnotes. One aside per distinct footnote
             (deduped by uriid+frag), in first-reference order. The body is
             pulled from the referenced footnotes document via document(). -->
        <xsl:variable name="footnotes" select="//xref[@documenttype = $note-types]"/>
        <xsl:if test="exists($footnotes)">
          <section epub:type="footnotes" role="doc-endnotes" class="footnotes">
            <xsl:for-each-group select="$footnotes" group-by="concat(@uriid,'-',@frag)">
              <xsl:apply-templates select="." mode="footnote-body"/>
            </xsl:for-each-group>
          </section>
        </xsl:if>

        <!-- Hidden copies of the glossary definitions and bibliography entries referenced in
             THIS document, as popup targets (popups require an aside in the same file).
             One aside per distinct term/citation, in first-reference order. -->
        <xsl:variable name="terms"
                      select="if (exists($glossary-type)) then //xref[@documenttype = $glossary-type] else ()"/>
        <xsl:variable name="citations"
                      select="if (exists($bibliography-type)) then //xref[@documenttype = $bibliography-type] else ()"/>
        <xsl:if test="exists($terms) or exists($citations)">
          <section class="popup-notes">
            <xsl:for-each-group select="$terms" group-by="@uriid">
              <xsl:apply-templates select="." mode="glossary-note"/>
            </xsl:for-each-group>
            <xsl:for-each-group select="$citations" group-by="concat(@uriid,'-',@frag)">
              <xsl:apply-templates select="." mode="biblio-note"/>
            </xsl:for-each-group>
          </section>
        </xsl:if>
      </body>
    </html>
  </xsl:template>

  <!-- =====================================================================
     DOCUMENTS: transcluded content, preserve IDs for xref anchoring
     ===================================================================== -->
  <xsl:template match="document">
    <div class="document" id="{@id}-default">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <!-- =====================================================================
       SECTIONS: group content inside the article
       ===================================================================== -->
  <xsl:template match="section">
    <section>
      <xsl:if test="xref-fragment[psf:is-glossary(.)]">
        <xsl:attribute name="epub:type">glossary</xsl:attribute>
        <xsl:attribute name="role">doc-glossary</xsl:attribute>
      </xsl:if>
      <xsl:if test="properties-fragment[psf:is-biblioentry(.)]">
        <xsl:attribute name="epub:type">bibliography</xsl:attribute>
        <xsl:attribute name="role">doc-bibliography</xsl:attribute>
      </xsl:if>
      <xsl:if test="@title">
        <xsl:attribute name="aria-label" select="@title"/>
      </xsl:if>
      <xsl:if test="@fragmenttype">
        <xsl:attribute name="data-fragmenttype" select="@fragmenttype"/>
      </xsl:if>
      <!-- Consecutive bibliography entries are grouped in a list -->
      <xsl:for-each-group select="*" group-adjacent="psf:is-biblioentry(.)">
        <xsl:choose>
          <xsl:when test="current-grouping-key()">
            <ol class="bibliography" role="list">
              <xsl:apply-templates select="current-group()" mode="bibliography"/>
            </ol>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="current-group()"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each-group>
    </section>
  </xsl:template>

  <!-- =====================================================================
       FRAGMENTS: semantic wrappers, preserve IDs for xref anchoring
       ===================================================================== -->
  <xsl:template match="fragment">
    <xsl:if test="not(tokenize(@labels,',') = $hide-labels or @type = $hide-types)">
      <div id="{@id}">
        <xsl:attribute name="class">
          <xsl:text>fragment</xsl:text>
          <xsl:if test="@labels">
            <xsl:text> </xsl:text>
            <xsl:value-of select="replace(@labels, ',', ' ')"/>
          </xsl:if>
        </xsl:attribute>
        <xsl:if test="@type">
          <xsl:attribute name="data-fragment-type" select="@type"/>
        </xsl:if>
        <xsl:apply-templates/>
      </div>
    </xsl:if>
  </xsl:template>

  <!-- Properties-fragments → rendered as definition lists.
       This handles metadata blocks (author, title, etc.) -->
  <xsl:template match="properties-fragment">
    <xsl:if test="not(tokenize(@labels,',') = $hide-labels or @type = $hide-types)">
      <xsl:if test="property[@value or value or xref or link]">
        <dl class="properties-fragment" id="{@id}">
          <xsl:if test="@type">
            <xsl:attribute name="data-properties-type" select="@type"/>
          </xsl:if>
          <xsl:apply-templates select="property[@value or value or xref or link or para]" mode="property"/>
        </dl>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template match="property" mode="property">
    <dt>
      <xsl:value-of select="(@title, @name)[1]"/>
    </dt>
    <dd>
      <xsl:choose>
        <xsl:when test="value">
          <xsl:for-each select="value">
            <xsl:if test="position() &gt; 1">, </xsl:if>
            <xsl:value-of select="."/>
          </xsl:for-each>
        </xsl:when>
        <xsl:when test="xref">
          <xsl:apply-templates select="xref"/>
        </xsl:when>
        <xsl:when test="link">
          <xsl:apply-templates select="link"/>
        </xsl:when>
        <xsl:when test="para">
          <xsl:apply-templates select="para"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@value"/>
        </xsl:otherwise>
      </xsl:choose>
    </dd>
  </xsl:template>

  <!-- =====================================================================
       BLOCK LABELS: semantic sections
       ===================================================================== -->
  <xsl:template match="block">
    <section class="{@label}">
      <xsl:apply-templates/>
    </section>
  </xsl:template>

  <!-- =====================================================================
       HEADINGS
       ===================================================================== -->
  <xsl:template match="heading">
    <xsl:variable name="level"
                  select="xs:integer((@level, 1)[1])"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema"/>
    <xsl:element name="h{if ($level &gt; 6) then 6 else $level}"
                 namespace="http://www.w3.org/1999/xhtml">
      <xsl:if test="@id">
        <xsl:attribute name="id" select="@id"/>
      </xsl:if>
      <xsl:if test="@prefix">
        <span class="heading-prefix">
          <xsl:value-of select="@prefix"/>
          <xsl:text> </xsl:text>
        </span>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <!-- =====================================================================
       PARAGRAPHS & INLINE TEXT
       ===================================================================== -->
  <xsl:template match="para">
    <p>
      <xsl:if test="@indent">
        <xsl:attribute name="class">
          <xsl:text>indent-</xsl:text>
          <xsl:value-of select="@indent"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@prefix">
        <span class="para-prefix">
          <xsl:value-of select="@prefix"/>
          <xsl:text> </xsl:text>
        </span>
      </xsl:if>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="preformat">
    <pre><xsl:apply-templates/></pre>
  </xsl:template>

  <!-- Inline formatting -->
  <xsl:template match="bold">
    <strong><xsl:apply-templates/></strong>
  </xsl:template>

  <xsl:template match="italic">
    <em><xsl:apply-templates/></em>
  </xsl:template>

  <xsl:template match="underline">
    <span class="underline"><xsl:apply-templates/></span>
  </xsl:template>

  <xsl:template match="sub">
    <sub><xsl:apply-templates/></sub>
  </xsl:template>

  <xsl:template match="sup">
    <sup><xsl:apply-templates/></sup>
  </xsl:template>

  <xsl:template match="monospace">
    <code><xsl:apply-templates/></code>
  </xsl:template>

  <xsl:template match="br">
    <br/>
  </xsl:template>

  <!-- Inline labels (content-inline) → <span class="label"> -->
  <xsl:template match="inline">
    <span class="{@label}">
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <!-- =====================================================================
       LISTS
       ===================================================================== -->
  <xsl:template match="list">
    <ul>
      <xsl:if test="@role">
        <xsl:attribute name="class">
          <xsl:text>role-</xsl:text>
          <xsl:value-of select="@role"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="nlist">
    <ol>
      <xsl:if test="@role">
        <xsl:attribute name="class">
          <xsl:text>role-</xsl:text>
          <xsl:value-of select="@role"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@start">
        <xsl:attribute name="start" select="@start"/>
      </xsl:if>
      <xsl:apply-templates/>
    </ol>
  </xsl:template>

  <xsl:template match="item">
    <li><xsl:apply-templates/></li>
  </xsl:template>

  <!-- =====================================================================
       TABLES
       ===================================================================== -->
  <xsl:template match="table">
    <!-- Column count drives wide-table handling. Comparison tables here are
         10-col jurisdiction grids; cramming those into a reflow page is what
         overlaps. >=6 cols → scroll + fixed layout + smaller font (see epub.css). -->
    <xsl:variable name="ncols"
                  select="if (col) then count(col)
                          else max(for $r in row return count($r/cell))"/>
    <div class="table-scroll">
      <table>
        <xsl:attribute name="class">
          <xsl:if test="@role">role-<xsl:value-of select="@role"/><xsl:text> </xsl:text></xsl:if>
          <xsl:text>cols-</xsl:text><xsl:value-of select="$ncols"/>
          <xsl:if test="$ncols &gt;= 6"><xsl:text> wide</xsl:text></xsl:if>
        </xsl:attribute>
        <xsl:if test="caption">
          <caption><xsl:apply-templates select="caption/node()"/></caption>
        </xsl:if>
        <xsl:if test="col">
          <colgroup>
            <xsl:for-each select="col">
              <col>
                <xsl:if test="@width">
                  <xsl:attribute name="style">width: <xsl:value-of select="@width"/>;</xsl:attribute>
                </xsl:if>
              </col>
            </xsl:for-each>
          </colgroup>
        </xsl:if>
        <xsl:if test="row[@part='header']">
          <thead>
            <xsl:apply-templates select="row[@part='header']"/>
          </thead>
        </xsl:if>
        <tbody>
          <xsl:apply-templates select="row[not(@part) or @part='body']"/>
        </tbody>
        <xsl:if test="row[@part='footer']">
          <tfoot>
            <xsl:apply-templates select="row[@part='footer']"/>
          </tfoot>
        </xsl:if>
      </table>
    </div>
  </xsl:template>

  <xsl:template match="row">
    <tr><xsl:apply-templates/></tr>
  </xsl:template>

  <xsl:template match="cell">
    <xsl:choose>
      <xsl:when test="parent::row/@part='header' or @role='header'">
        <th>
          <xsl:call-template name="cell-attrs"/>
          <xsl:apply-templates/>
        </th>
      </xsl:when>
      <xsl:otherwise>
        <td>
          <xsl:call-template name="cell-attrs"/>
          <xsl:apply-templates/>
        </td>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="hcell">
    <th>
      <xsl:call-template name="cell-attrs"/>
      <xsl:apply-templates/>
    </th>
  </xsl:template>

  <xsl:template name="cell-attrs">
    <xsl:if test="@colspan"><xsl:attribute name="colspan" select="@colspan"/></xsl:if>
    <xsl:if test="@rowspan"><xsl:attribute name="rowspan" select="@rowspan"/></xsl:if>
    <xsl:if test="@align"><xsl:attribute name="style">text-align: <xsl:value-of select="@align"/>;</xsl:attribute></xsl:if>
  </xsl:template>

  <!-- =====================================================================
       IMAGES
       Images source path points somewhere like ../images/house.png
       but in the ePub they live at OEBPS/media/ and XHTML at OEBPS/xhtml/,
       so we rewrite to ../media/{filename}.
       ===================================================================== -->
  <xsl:template match="image">
    <img>
      <xsl:attribute name="src">
        <xsl:text>../media/</xsl:text>
        <xsl:value-of select="tokenize(@src, '/')[last()]"/>
      </xsl:attribute>
      <xsl:attribute name="alt">
        <xsl:value-of select="(@alt, ancestor::image/@alt, '')[1]"/>
      </xsl:attribute>
      <xsl:if test="@width">
        <xsl:attribute name="width" select="@width"/>
      </xsl:if>
      <xsl:if test="@height">
        <xsl:attribute name="height" select="@height"/>
      </xsl:if>
    </img>
  </xsl:template>

  <!-- Image wrapped in para → figure -->
  <xsl:template match="para[count(*) = 1 and image and normalize-space() = '']">
    <figure>
      <xsl:apply-templates select="image"/>
    </figure>
  </xsl:template>

  <!-- =====================================================================
       XREFS (cross-references)
         - internal (this document): href="#fragment-id"
         - cross-document: href="other-file.xhtml#fragment-id"
         - external (http/https): external link styling
       ===================================================================== -->

  <!-- Inline xref -->
  <xsl:template match="xref">
    <xsl:call-template name="render-xref">
      <xsl:with-param name="display-content">
        <xsl:choose>
          <xsl:when test="node()">
            <xsl:apply-templates/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="(@urititle, @title, @href)[1]"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- Inline xref with embedded content (e.g. an alternate xref to a fragment): the embedded
       content can't go inside the link, so the link text is the xref title template with
       '{fragment}' replaced by the fragment, or the URI title if other placeholders remain. -->
  <xsl:template match="xref[document or fragment or properties-fragment or xref-fragment]" priority="1">
    <xsl:variable name="title" select="replace(string(@title), '\{fragment\}', string(@frag))"/>
    <xsl:call-template name="render-xref">
      <xsl:with-param name="display-content">
        <xsl:value-of select="if ($title != '' and not(contains($title, '{'))) then $title
                              else (@urititle, @href)[1]"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- Block xref -->
  <xsl:template match="blockxref">
    <xsl:choose>
      <!-- Ignore embeded content as it is already in the TOC -->
      <xsl:when test="@type='embed'" />

      <!-- Include transcluded content -->
      <xsl:when test="@type='transclude'">
        <xsl:apply-templates />
      </xsl:when>

      <!-- External web URL not pointing to an asset → external link -->
      <xsl:when test="starts-with(@href, 'http')">
        <p>
          <xsl:call-template name="render-external-link"/>
        </p>
      </xsl:when>

      <!-- Default: internal or cross-file anchor -->
      <xsl:otherwise>
        <p>
          <xsl:call-template name="render-xref">
            <xsl:with-param name="display-content">
              <xsl:value-of select="(@urititle, @title, @href)[1]"/>
            </xsl:with-param>
          </xsl:call-template>
        </p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Named xref renderer — handles href rewriting -->
  <xsl:template name="render-xref">
    <xsl:param name="display-content"/>
    <a>
      <xsl:attribute name="class">
        <xsl:text>xref</xsl:text>
        <xsl:if test="@labels">
          <xsl:text> </xsl:text>
          <xsl:value-of select="replace(@labels, ',', ' ')"/>
        </xsl:if>
      </xsl:attribute>
      <xsl:attribute name="href">
        <xsl:call-template name="rewrite-href"/>
      </xsl:attribute>
      <xsl:apply-templates select="$display-content"/>
    </a>
  </xsl:template>

  <!-- Href rewriting: PSML hrefs → XHTML hrefs.
       Examples:
         ../components/001-lesson.psml#fragment-id → 001-lesson.xhtml#fragment-id
         #fragment-id → #doc-{thisdoc-id}-fragment-id
         https://... → https://... (unchanged)
  -->
  <xsl:template name="rewrite-href">
    <xsl:variable name="href" select="@href"/>
    <xsl:variable name="frag" select="@frag"/>
    <xsl:variable name="target-uriid" select="@uriid"/>
    <xsl:choose>
      <!-- External URL -->
      <xsl:when test="starts-with($href, 'http')">
        <xsl:value-of select="$href"/>
      </xsl:when>
      <!-- Cross-document xref: filename.psml → filename.xhtml -->
      <xsl:when test="contains($href, '.psml')">
        <xsl:variable name="filename" select="tokenize($href, '/')[last()]"/>
        <xsl:value-of select="replace($filename, '\.psml$', '.xhtml')"/>
        <xsl:text>#</xsl:text>
        <xsl:value-of select="$target-uriid"/>
        <xsl:text>-</xsl:text>
        <xsl:value-of select="$frag"/>
      </xsl:when>
      <!-- Internal fragment-only xref -->
      <xsl:otherwise>
        <xsl:text>#</xsl:text>
        <xsl:value-of select="$target-uriid"/>
        <xsl:text>-</xsl:text>
        <xsl:value-of select="$frag"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- =====================================================================
       EXTERNAL RESOURCES (S3 PDFs, videos, interactives)
       Asset documents with link properties → styled external link
       ===================================================================== -->
  <xsl:template name="render-external-link">
    <a class="external-resource" target="_blank" rel="noopener">
      <xsl:attribute name="href" select="@href"/>
      <xsl:value-of select="(@urititle, @title, 'External resource')[1]"/>
    </a>
  </xsl:template>

  <!-- =====================================================================
       LINKS (explicit <link> elements)
       ===================================================================== -->
  <xsl:template match="link">
    <a href="{@href}">
      <xsl:attribute name="class">
        <xsl:text>link</xsl:text>
        <xsl:if test="@labels">
          <xsl:text> </xsl:text>
          <xsl:value-of select="replace(@labels, ',', ' ')"/>
        </xsl:if>
      </xsl:attribute>
      <xsl:apply-templates/>
    </a>
  </xsl:template>

  <!-- =====================================================================
       MATH (MathML passthrough)
       The ps:process converts inline math labels (asciimath, tex) to
       xref[@type='math']/media-fragment containing MathML which is passed through.
       ===================================================================== -->
  <xsl:template match="xref[@type='math']">
    <span class="mathml">
      <xsl:apply-templates select="media-fragment/node()"/>
    </span>
  </xsl:template>

  <!-- Direct MathML (if already present) -->
  <xsl:template match="m:math | math" xmlns:m="http://www.w3.org/1998/Math/MathML">
    <xsl:copy-of select="."/>
  </xsl:template>

  <!-- =====================================================================
       XREF-FRAGMENT: wrapping fragment for xrefs.
       After ps:process, this typically contains resolved/embedded content.
       ===================================================================== -->
  <xsl:template match="xref-fragment">
    <div class="xref-fragment" id="{@id}">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <!-- Media-fragment - currently ignored -->
  <xsl:template match="media-fragment">
  </xsl:template>

  <!-- =====================================================================
       DOCUMENTINFO, FRAGMENTINFO, METADATA: suppressed in rendered output.
       Metadata is captured in <head>; structural info isn't user-facing.
       ===================================================================== -->
  <xsl:template match="documentinfo | fragmentinfo | metadata | reversexrefs | reversexref | locator"/>

  <!-- Nested <document> (resolved image metadata): suppress entirely -->
  <xsl:template match="image/document"/>

  <!-- =====================================================================
       DEFAULT: copy text, apply templates to children
       ===================================================================== -->
  <xsl:template match="text()">
    <xsl:copy/>
  </xsl:template>

  <!-- Swallow unknown elements gracefully: emit their children but
       log a warning comment. This is safer than pass-through for XHTML
       validity (PSML elements aren't valid XHTML). -->
  <xsl:template match="*">
    <!--
    <xsl:message>[psml-to-xhtml] No template for element: <xsl:value-of select="name()"/> (path: <xsl:value-of select="string-join(ancestor-or-self::*/name(), '/')"/>)</xsl:message>
    -->
    <xsl:apply-templates/>
  </xsl:template>

  <!-- =====================================================================
       GLOSSARY (popup model, only when <glossary> is configured)
         definitions = xref-fragment[@type=glossary/@fragmenttype] in the glossary
                       document, transcluding one definition document per term.
                       Rendered as a dl: the term is the heading of the definition
                       document, the definition the rest of its content.
         references  = xref[@documenttype=glossary/@documenttype] to a definition
                       document, rendered as a noteref to the definition so that
                       popup-capable readers show it in a popup.
       ===================================================================== -->

  <!-- Whether the xref-fragment contains the definitions of the glossary -->
  <xsl:function name="psf:is-glossary" as="xs:boolean" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:param name="fragment" as="element()"/>
    <xsl:sequence select="exists($glossary-type) and exists($glossary-fragment-type)
                          and $fragment/@type = $glossary-fragment-type
                          and root($fragment)/document/@type = $glossary-type"/>
  </xsl:function>

  <!-- Definitions list -->
  <xsl:template match="xref-fragment[psf:is-glossary(.)]" priority="1">
    <dl class="glossary" id="{@id}">
      <xsl:apply-templates select="blockxref/document" mode="glossary"/>
    </dl>
  </xsl:template>

  <!-- One term and its definition per definition document -->
  <xsl:template match="document" mode="glossary">
    <xsl:variable name="term" select="(.//heading)[1]"/>
    <xsl:variable name="term-fragment" select="$term/ancestor::fragment[1]"/>
    <dt id="{@id}-default" epub:type="glossterm">
      <dfn><xsl:apply-templates select="$term/node()"/></dfn>
    </dt>
    <dd id="{@id}-def" epub:type="glossdef">
      <xsl:apply-templates select="section/*[not(. is $term-fragment)]"/>
    </dd>
  </xsl:template>

  <!-- Reference to a glossary term: a noteref to the hidden copy of the definition at the end of
       this file (see popup-notes), since popups only work for asides in the same file.
       Inside a popup note, the reference is plain text (the term may not be in this file). -->
  <xsl:template match="xref[exists($glossary-type) and @documenttype = $glossary-type]" priority="2">
    <xsl:param name="in-note" select="false()" tunnel="yes"/>
    <xsl:variable name="text">
      <xsl:choose>
        <xsl:when test="node()">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="(@urititle, @title)[1]"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$in-note">
        <xsl:copy-of select="$text"/>
      </xsl:when>
      <xsl:otherwise>
        <a epub:type="noteref" role="doc-glossref" class="glossref" href="#gl-{@uriid}">
          <xsl:copy-of select="$text"/>
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Hidden copy of the definition of a glossary term (popup target); context = first reference -->
  <xsl:template match="xref" mode="glossary-note">
    <xsl:variable name="uriid" select="string(@uriid)"/>
    <xsl:variable name="source" select="if (contains(@href, '.psml')) then document(@href, .) else root(.)"/>
    <xsl:variable name="definition" select="($source//blockxref/document[@id = $uriid])[1]"/>
    <xsl:variable name="term" select="($definition//heading)[1]"/>
    <xsl:variable name="term-fragment" select="$term/ancestor::fragment[1]"/>
    <aside epub:type="footnote" class="glossary-note" id="gl-{$uriid}">
      <xsl:choose>
        <xsl:when test="$definition">
          <p class="glossary-term"><dfn><xsl:apply-templates select="$term/node()">
            <xsl:with-param name="in-note" select="true()" tunnel="yes"/>
          </xsl:apply-templates></dfn></p>
          <!-- Fragment content without the fragment wrappers (no duplicate IDs) -->
          <xsl:apply-templates select="$definition/section/*[not(. is $term-fragment)]/node()">
            <xsl:with-param name="in-note" select="true()" tunnel="yes"/>
          </xsl:apply-templates>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>[glossary] unresolved: <xsl:value-of select="@href"/> #<xsl:value-of select="$uriid"/></xsl:message>
          <p class="glossary-term"><dfn><xsl:value-of select="."/></dfn></p>
        </xsl:otherwise>
      </xsl:choose>
    </aside>
  </xsl:template>

  <!-- =====================================================================
       BIBLIOGRAPHY (popup model, only when <bibliography> is configured)
         entries   = properties-fragment in the bibliography document with the
                     configured item or link property, rendered as an ol/li with
                     the item property content (or the link text as fallback).
         citations = xref[@documenttype=bibliography/@documenttype] rendered as a
                     noteref to the entry, with the link text of the entry.
       ===================================================================== -->

  <!-- Whether the element is an entry of the bibliography -->
  <xsl:function name="psf:is-biblioentry" as="xs:boolean" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xsl:param name="element" as="element()"/>
    <xsl:sequence select="exists($bibliography-type)
                          and $element/self::properties-fragment
                          and root($element)/document/@type = $bibliography-type
                          and exists($element/property[@name = ($bibliography-item, $bibliography-link)])
                          and not(tokenize($element/@labels, ',') = $hide-labels or $element/@type = $hide-types)"/>
  </xsl:function>

  <!-- Bibliography entry -->
  <xsl:template match="properties-fragment" mode="bibliography">
    <xsl:variable name="item" select="property[@name = $bibliography-item][1]"/>
    <li id="{@id}" epub:type="biblioentry">
      <xsl:choose>
        <xsl:when test="$item/*">
          <xsl:apply-templates select="$item/*"/>
        </xsl:when>
        <xsl:when test="normalize-space($item/@value) != ''">
          <p><xsl:value-of select="$item/@value"/></p>
        </xsl:when>
        <xsl:otherwise>
          <p><xsl:value-of select="property[@name = $bibliography-link][1]/@value"/></p>
        </xsl:otherwise>
      </xsl:choose>
    </li>
  </xsl:template>

  <!-- Citation: the link text of the entry (keeping the brackets of the citation) -->
  <!-- The citation links to the hidden copy of the entry at the end of this file (see popup-notes).
       Inside a popup note, the citation is plain text. -->
  <xsl:template match="xref[exists($bibliography-type) and @documenttype = $bibliography-type]" priority="2">
    <xsl:param name="in-note" select="false()" tunnel="yes"/>
    <xsl:variable name="link-text" select="document(@href, .)//properties-fragment[
      @id=concat(current()/@uriid,'-',current()/@frag)]/property[@name = $bibliography-link][1]/@value" />
    <xsl:variable name="text">
      <xsl:choose>
        <xsl:when test="not($link-text)">
          <xsl:value-of select="." />
        </xsl:when>
        <xsl:when test="starts-with(., '(')">
          <xsl:value-of select="concat('(',$link-text,')')" />
        </xsl:when>
        <xsl:when test="starts-with(., '[')">
          <xsl:value-of select="concat('[',$link-text,']')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$link-text" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$in-note">
        <xsl:value-of select="$text"/>
      </xsl:when>
      <xsl:otherwise>
        <a epub:type="noteref" role="doc-biblioref" class="biblioref" href="#bib-{@uriid}-{@frag}">
          <xsl:value-of select="$text"/>
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Hidden copy of a bibliography entry (popup target); context = first citation -->
  <xsl:template match="xref" mode="biblio-note">
    <xsl:variable name="id" select="concat(@uriid, '-', @frag)"/>
    <xsl:variable name="entry" select="(document(@href, .)//properties-fragment[@id = $id])[1]"/>
    <xsl:variable name="item" select="$entry/property[@name = $bibliography-item][1]"/>
    <aside epub:type="footnote" class="biblio-note" id="bib-{$id}">
      <xsl:choose>
        <xsl:when test="$item/*">
          <xsl:apply-templates select="$item/*">
            <xsl:with-param name="in-note" select="true()" tunnel="yes"/>
          </xsl:apply-templates>
        </xsl:when>
        <xsl:when test="normalize-space($item/@value) != ''">
          <p><xsl:value-of select="$item/@value"/></p>
        </xsl:when>
        <xsl:when test="$entry">
          <p><xsl:value-of select="$entry/property[@name = $bibliography-link][1]/@value"/></p>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>[bibliography] unresolved: <xsl:value-of select="@href"/> #<xsl:value-of select="$id"/></xsl:message>
          <p><xsl:value-of select="."/></p>
        </xsl:otherwise>
      </xsl:choose>
    </aside>
  </xsl:template>

  <!-- =====================================================================
       FOOTNOTES (popup model)
       Confirmed shape (process/OBH0002/components/*.psml):
         ref  = inline <xref documenttype="footnotes" config="footnote"
                        type="alternate" uriid="U" frag="F" href="chNN-footnotes.psml">marker</xref>
                — NOT transcluded; content is just the marker text (e.g. "f3").
         body = <fragment id="{U}-{F}"><para>…</para></fragment> in that doc.

       For popups the noteref and its <aside epub:type="footnote"> must be in the
       SAME document, so the body is pulled in via document(@href,.) and inlined
       at end of the lesson (see root template). The footnotes documents are no
       longer transformed as standalone pages (excluded in the ANT transform).

       RUNTIME DEPENDENCY: document(@href, .) must resolve the sibling processed
       footnotes .psml at transform time (it lives in the same components/ dir).
       The otherwise branch fails loud and degrades to the marker text.
       ===================================================================== -->

  <!-- Inline reference → same-doc noteref. id uses the xref's own @id (unique
       per occurrence); target is the per-footnote id {uriid}-{frag}. -->
  <xsl:template match="xref[@documenttype = $note-types]" priority="2">
    <a epub:type="noteref" role="doc-noteref" class="noteref"
       id="fnref-{@id}" href="#fn-{@uriid}-{@frag}">
      <sup><xsl:value-of select="@frag"/></sup>
    </a>
  </xsl:template>

  <!-- Body (one per distinct footnote; context node = first reference of the
       group). Pulled from the footnotes document via document(). -->
  <xsl:template match="xref[@documenttype = $note-types]" mode="footnote-body">
    <xsl:variable name="fnid"   select="concat(@uriid, '-', @frag)"/>
    <xsl:variable name="marker" select="@frag"/>
    <xsl:variable name="refid"  select="@id"/>
    <xsl:variable name="body"   select=".//fragment"/>
    <aside epub:type="footnote" role="doc-footnote" id="fn-{$fnid}">
      <xsl:choose>
        <xsl:when test="$body/para">
          <xsl:for-each select="$body/para">
            <p class="footnote">
              <xsl:if test="position() = 1">
                <span class="fn-marker"><xsl:value-of select="$marker"/><xsl:text> </xsl:text></span>
              </xsl:if>
              <xsl:apply-templates select="node()"/>
              <xsl:if test="position() = last()">
                <xsl:text> </xsl:text>
                <a epub:type="backlink" role="doc-backlink" href="#fnref-{$refid}">&#8617;</a>
              </xsl:if>
            </p>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>[footnote] unresolved: <xsl:value-of select="@href"/> #<xsl:value-of select="$fnid"/></xsl:message>
          <p class="footnote">
            <span class="fn-marker"><xsl:value-of select="$marker"/><xsl:text> </xsl:text></span>
            <xsl:text> </xsl:text>
            <a epub:type="backlink" role="doc-backlink" href="#fnref-{$refid}">&#8617;</a>
          </p>
        </xsl:otherwise>
      </xsl:choose>
    </aside>
  </xsl:template>

</xsl:stylesheet>