# EPUB import and export API

About this library
------------------

This library provides Apache ANT tasks to:
- import a publication in EPUB format into PageSeeder documents
- export PageSeeder PSML documents into the EPUB format

Dependencies
------------

The ANT task should only depend on ANT 1.10.
It does also require an XSLT2 processor (For example Saxon)

Usage
-----

The export task requires PSML from a PageSeeder publication that has been exported
using `processpublication='true` and processed using `<xrefs>` and
`<images src="filename" location="${images}"/>` for example:

```xml
<ps:export src="${ps.config.default.uri.path}"
           dest="${download}"
           xrefdepth="10"
           processpublication="true">
  <xrefs types="embed,transclude,math"/>
</ps:export>

<ps:process src="${download}"
        dest="${process}">
  <xrefs types="alternate" />
  <images src="filename" location="${images}"/>
</ps:process>

<property name="epub.file"
          value="${working}/${ps.config.default.uri.filename.no.ext}.epub"/>
<path id="epub.classpath">
  <fileset dir="lib"><include name="*.jar" /></fileset>
</path>
<taskdef name="export-epub"
         classname="org.pageseeder.epub.ant.ExportTask"
         classpathref="epub.classpath"/>
<export-epub src="${process}/${ps.config.default.uri.filename}"
             dest="${epub.file}"
             config="epub-export-config.xml"
             media="${images}"
             working="${working}/epub" />
```

An `export-epub/@css` attribute containing a path replaces the default CSS:
`pso-epub-core/src/main/resources/org/pageseeder/epub/export/static/OEBPS/styles/epub.css`

In this example the following jar files must be in the `lib` folder at the same level
as the ANT script file:
- `pso-epub-ant-[version].jar`
- `pso-epub-core-[version].jar`

Also the `epub-export-config.xml` file with following content must be in the
same folder as the ANT script file (unless `export-epub/@config` is omitted):

```xml
<!-- All elements except the root <config> element are optional. -->
<config>

  <!-- Each dc or meta element is optional and can have a @value and/or @property
       with @property overriding @value with the value from the PSML property/@value of that name.
       If @multiple='true' then values are taken from the PSML property/value.
       The dc/@name only supports the names listed but meta/@name can be anything. -->
  <metadata>
    <dc name="isbn" property="epub-config-isbn"/>
    <dc name="title" property="epub-config-title"/>
    <dc name="creator" property="epub-config-creator" multiple="true"/>
    <dc name="language" property="epub-config-language"/>
    <dc name="date" property="epub-config-date"/>
    <dc name="publisher" value="Example Press" />
    <dc name="subject" value="General" property="subject" />
    <dc name="rights" property="epub-config-rights" />
    <meta name="dcterms:conformsTo" value="EPUB Accessibility 1.1 - WCAG 2.1 Level AA" />
    <meta name="schema:accessMode" property="epub-config-access-mode" multiple="true" />
    <meta name="schema:accessModeSufficient" property="epub-config-access-mode-sufficient" multiple="true" />
    <meta name="schema:accessibilityFeature" property="epub-config-accessibility-feature" multiple="true" />
    <meta name="schema:accessibilityHazard" property="epub-config-accessibility-hazard" multiple="true" />
    <meta name="schema:accessibilityControl" property="epub-config-accessibility-control" multiple="true" />
    <meta name="schema:accessibilityAPI" property="epub-config-accessibility-api" multiple="true" />
    <meta name="schema:accessibilitySummary" property="epub-config-accessibility-summary" />
    <meta name="a11y:certifiedBy" property="epub-config-certified-by" />
    <meta name="a11y:certifierCredential" property="epub-config-certifier-credential" />
    <meta name="a11y:certifierReport" property="epub-config-certifier-report" />
  </metadata>

  <!-- The PSML xref/@documenttype for endnotes and footnotes -->
  <endnotes documenttype="endnotes" />
  <footnotes documenttype="footnotes" />

  <!-- The PSML xref/@documenttype for terms and xref-fragment/@type or fragment/@type for definitions -->
  <glossary documenttype="glossary" fragmenttype="definitions" />

  <!-- The PSML xref/@documenttype for citations and properties for bibliography items -->
  <bibliography documenttype="bibliography"
                linkproperty="link-text"
                itemproperty="description"/>

  <!-- Don't output to the EPUB any PSML fragment or property-fragment
       with these labels or types-->
  <fragment>
    <hide label="print" />
    <hide type="epub-config" />
    <hide type="epub-a11y" />
  </fragment>
</config>
```

See `pso-epub-core/src/main/resources/org/pageseeder/epub/export/epub-export-config.xsd`

Copyright (c) 1999-2026 Allette Systems Pty Ltd - All Rights Reserved.