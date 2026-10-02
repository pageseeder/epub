<!--
  ~ Copyright (c) 1999-2026. Allette Systems Pty Ltd
  ~
  ~ Export configuration shared by the export stylesheets.
  ~
  ~ The configuration file is passed as a URL with the 'config-url' parameter.
  ~ Every option is off unless it is specified in the configuration.
-->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:psf="http://www.pageseeder.com/function"
                xpath-default-namespace=""
                exclude-result-prefixes="#all">

  <!-- URL of the export configuration -->
  <xsl:param name="config-url" select="''"/>

  <!-- The export configuration (empty if none) -->
  <xsl:variable name="config" select="if ($config-url != '') then document($config-url)/config else ()"/>

  <!-- Document types of the xrefs rendered as footnotes/endnotes -->
  <xsl:variable name="note-types" select="($config/footnotes/@documenttype, $config/endnotes/@documenttype)/string()"/>

  <!-- Document type of the xrefs to glossary terms and fragment type of the definitions -->
  <xsl:variable name="glossary-type"          select="$config/glossary/@documenttype/string()"/>
  <xsl:variable name="glossary-fragment-type" select="$config/glossary/@fragmenttype/string()"/>

  <!-- Document type of the xrefs to citations and names of the bibliography item properties -->
  <xsl:variable name="bibliography-type"   select="$config/bibliography/@documenttype/string()"/>
  <xsl:variable name="bibliography-link"   select="$config/bibliography/@linkproperty/string()"/>
  <xsl:variable name="bibliography-item"   select="$config/bibliography/@itemproperty/string()"/>

  <!-- Labels and types of the fragments not output to the EPUB -->
  <xsl:variable name="hide-labels" select="$config/fragment/hide/@label/string()"/>
  <xsl:variable name="hide-types"  select="$config/fragment/hide/@type/string()"/>

  <!--
    Returns the values of a metadata dc/meta entry from the configuration.

    The value of the PSML property named by @property overrides @value, unless it is empty.
    If @multiple='true', the values are taken from the PSML property/value elements.
    Empty values are ignored.

    @param entry The dc or meta element from the configuration
    @param doc   The PSML document containing the properties
  -->
  <xsl:function name="psf:config-values" as="xs:string*">
    <xsl:param name="entry" as="element()?"/>
    <xsl:param name="doc"   as="node()"/>
    <xsl:variable name="property" select="if ($entry/@property) then ($doc//property[@name = $entry/@property])[1] else ()"/>
    <xsl:variable name="from-property" as="xs:string*"
                  select="if ($entry/@multiple = 'true')
                          then (for $v in $property/value return normalize-space($v))[. != '']
                          else normalize-space(string(($property/@value, $property/link/@href)[1]))[. != '']"/>
    <xsl:sequence select="if (exists($from-property)) then $from-property
                          else normalize-space(string($entry/@value))[. != '']"/>
  </xsl:function>

  <!--
    Returns the first value of the configured Dublin Core metadata or an empty string.

    @param name The dc/@name in the configuration
    @param doc  The PSML document containing the properties
  -->
  <xsl:function name="psf:dc" as="xs:string">
    <xsl:param name="name" as="xs:string"/>
    <xsl:param name="doc"  as="node()"/>
    <xsl:sequence select="string(psf:config-values($config/metadata/dc[@name = $name][1], $doc)[1])"/>
  </xsl:function>

  <!--
    Returns the unique identifier of the publication: the ISBN when valid, otherwise a
    PageSeeder URN based on the publication ID.

    @param doc  The PSML root document
  -->
  <xsl:function name="psf:identifier" as="xs:string">
    <xsl:param name="doc" as="node()"/>
    <xsl:variable name="isbn-digits" select="translate(psf:dc('isbn', $doc), '- ', '')"/>
    <xsl:sequence select="if (matches($isbn-digits, '^(978|979)[0-9]{10}$'))
                          then concat('urn:isbn:', $isbn-digits)
                          else concat('urn:pageseeder:', root($doc)/document/@publicationid)"/>
  </xsl:function>

</xsl:stylesheet>
