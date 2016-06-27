<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
  <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>
  <xsl:param name="versionParam" select="'1.0'"/>

  <xsl:template match="bug-reports">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
      <fo:layout-master-set>
        <fo:simple-page-master master-name="simpleA4" page-height="29.7cm" page-width="21cm" margin-top="2cm" margin-bottom="1cm" margin-left="2cm" margin-right="1cm">
          <fo:region-body region-name="xsl-region-body" margin="0.5cm" margin-top="3cm"/>
          <fo:region-before region-name="xsl-region-before" margin="0.5cm" extent="3cm" display-align="before" />
        </fo:simple-page-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="simpleA4">
        <xsl:apply-templates select="report-header"/>
        <fo:flow flow-name="xsl-region-body">
            <xsl:apply-templates select="all-open-bugs"/>
            <xsl:apply-templates select="open-bugs"/>
        </fo:flow>
      </fo:page-sequence>

      <fo:page-sequence master-reference="simpleA4">
        <xsl:apply-templates select="report-header"/>
        <fo:flow flow-name="xsl-region-body">
          <fo:block font-size="12pt" space-after="5mm">Page 2</fo:block>
        </fo:flow>
      </fo:page-sequence>

      <fo:page-sequence master-reference="simpleA4">
        <xsl:apply-templates select="report-header"/>
        <fo:flow flow-name="xsl-region-body">
          <fo:block font-size="12pt" space-after="5mm">Page 3</fo:block>
          <fo:block id='end'/>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>

  <xsl:template match="report-header">
    <fo:static-content flow-name="xsl-region-before">
      <fo:block font-size="12pt" font-weight="bold" text-align="center" space-after="5mm" color="white" background-color="#365f91" border-color="black" border-width="1pt" border-style="solid">
        <fo:block>Customer Development Team</fo:block>
        <fo:block>Weekly Update, Week</fo:block>
        <fo:block>#<xsl:value-of select="week"/></fo:block>
        <fo:block><xsl:value-of select="date"/></fo:block>
        <fo:block text-align="outside">
          Page <fo:page-number font-style="normal" /> of <fo:page-number-citation ref-id='end'/>
        </fo:block>
      </fo:block>
    </fo:static-content>
  </xsl:template>


  <xsl:template match="all-open-bugs">
    <fo:block font-size="12pt" font-weight="bold">All Open Production Bugs *</fo:block>
    <fo:block font-size="10pt">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header color="white" background-color="#5b9bd5">
          <fo:table-cell border="solid 1px black" padding="1em" width="3cm">
            <fo:block text-align="justify" font-weight="bold">Priority</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">0-2 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">2-7 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">7-30 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">30-90 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">91-365 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Over 1 year</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Grand Total</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body>
          <xsl:apply-templates select="prioritized-bugs"/>
        </fo:table-body>
      </fo:table>
      <fo:block font-size="8pt" text-align="right">* As of Monday, 27-Jun-2016, 3:00PM GMT</fo:block>
      <fo:block font-size="8pt" text-align="right">** Excluding Hot Deploys</fo:block>
    </fo:block>
  </xsl:template>


  <xsl:template match="all-open-bugs/prioritized-bugs">
    <fo:table-row>
      <xsl:choose>
        <xsl:when test="priority = 'P1'"><xsl:attribute name="color">red</xsl:attribute></xsl:when>
        <xsl:when test="priority = 'P2'"><xsl:attribute name="color">blue</xsl:attribute></xsl:when>
        <xsl:when test="priority = 'Grand Total'"><xsl:attribute name="font-weight">bold</xsl:attribute></xsl:when>
      </xsl:choose>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="priority"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="period1"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="period2"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="period3"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="period4"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="period5"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="period6"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="total"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>


  <xsl:template match="open-bugs">
    <fo:block font-size="10pt" margin-bottom="0.5cm" margin-top="0.5cm" margin-left="0">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Bug ID</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Priority</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="7cm">
            <fo:block text-align="justify" font-weight="bold">Summary</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Opened</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Client</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid 1px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Product</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body>
          <xsl:apply-templates select="bug"/>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="open-bugs/bug">
    <fo:table-row>
      <xsl:if test="priority = 'P1'">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
      </xsl:if>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="id"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="priority"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="summary"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="opened"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="client"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid 1px black">
        <fo:block>
          <xsl:value-of select="product"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

</xsl:stylesheet>