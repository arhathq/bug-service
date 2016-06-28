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
          <xsl:apply-templates select="reporter-bugs-by-15-weeks"/>
          <xsl:apply-templates select="reporter-bugs-by-this-week"/>
          <xsl:apply-templates select="priority-bugs-by-this-week"/>
          <xsl:apply-templates select="open-bugs-by-product"/>
          <xsl:apply-templates select="top-asignees"/>
        </fo:flow>
      </fo:page-sequence>

      <fo:page-sequence master-reference="simpleA4">
        <xsl:apply-templates select="report-header"/>
        <fo:flow flow-name="xsl-region-body">
          <xsl:apply-templates select="week-summary-report"/>
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
          <fo:table-cell border="solid .5px black" padding="1em" width="3cm">
            <fo:block text-align="justify" font-weight="bold">Priority</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">0-2 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">2-7 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">7-30 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">30-90 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">91-365 Days</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
            <fo:block text-align="justify" font-weight="bold">Over 1 year</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" width="2cm">
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
      <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="priority"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
        <fo:block>
          <xsl:value-of select="period1"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
        <fo:block>
          <xsl:value-of select="period2"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
        <fo:block>
          <xsl:value-of select="period3"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
        <fo:block>
          <xsl:value-of select="period4"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
        <fo:block>
          <xsl:value-of select="period5"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
        <fo:block>
          <xsl:value-of select="period6"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em" text-align="right">
        <fo:block>
          <xsl:value-of select="total"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>


  <xsl:template match="open-bugs">
    <fo:block font-size="10pt" space-after=".5cm" space-before=".5cm" margin-left="0">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header font-weight="bold" text-align="justify">
          <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="1.5cm">
            <fo:block>Bug ID</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="1.5cm">
            <fo:block>Priority</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="8cm">
            <fo:block>Summary</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="2cm">
            <fo:block>Opened</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="2cm">
            <fo:block>Client</fo:block>
          </fo:table-cell>
          <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="2cm">
            <fo:block>Product</fo:block>
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
      <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="id"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="priority"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="summary"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="opened"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="client"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="product"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="reporter-bugs-by-15-weeks">
    <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">Last 15 Weeks - Bugs By Reporter’s Department:</fo:block>
    <fo:block font-size="10pt">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header background-color="#ddebf7" font-weight="bold" border-bottom="solid 0.5px #9cc2e5" text-align="right">
          <fo:table-cell width="5cm" padding-left=".5em" padding-top=".5em">
            <fo:block text-align="left">Department</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>CLOSED</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>INVALID</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>OPEN</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>Grand Total</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body text-align="right">
          <xsl:apply-templates select="reporter-bugs"/>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="reporter-bugs-by-15-weeks/reporter-bugs">
    <fo:table-row>
      <xsl:if test="reporter = 'Grand Total'">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="background-color">#ddebf7</xsl:attribute>
        <xsl:attribute name="border-top">solid 0.5px #9cc2e5</xsl:attribute>
      </xsl:if>
      <fo:table-cell text-align="left" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="reporter"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right="0.5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="closed"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right="0.5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="invalid"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right="0.5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="opened"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right="0.5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="total"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="reporter-bugs-by-this-week">
    <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">This Week Bugs by Reporter’s Department:</fo:block>
    <fo:block font-size="10pt">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header background-color="#ddebf7" font-weight="bold" border-bottom="solid .5px #9cc2e5" text-align="right">
          <fo:table-cell width="5cm" padding-left=".5em" padding-top=".5em">
            <fo:block text-align="left">Department</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>CLOSED</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>INVALID</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>OPEN</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>Grand Total</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body text-align="right">
          <xsl:apply-templates select="reporter-bugs"/>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="reporter-bugs-by-this-week/reporter-bugs">
    <fo:table-row>
      <xsl:if test="reporter = 'Grand Total'">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="background-color">#ddebf7</xsl:attribute>
        <xsl:attribute name="border-top">solid 0.5px #9cc2e5</xsl:attribute>
      </xsl:if>
      <fo:table-cell text-align="left" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="reporter"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="closed"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="invalid"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="opened"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="total"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="priority-bugs-by-this-week">
    <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">This Week’s New Bugs by Priorities:</fo:block>
    <fo:block font-size="10pt">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header background-color="#ddebf7" font-weight="bold" border-bottom="solid .5px #9cc2e5" text-align="right">
          <fo:table-cell width="5cm" padding-left=".5em" padding-top=".5em">
            <fo:block text-align="left">Department</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>CLOSED</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>INVALID</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>OPEN</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>Grand Total</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body text-align="right">
          <xsl:apply-templates select="priority-bugs"/>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="priority-bugs-by-this-week/priority-bugs">
    <fo:table-row>
      <xsl:if test="reporter = 'Grand Total'">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="background-color">#ddebf7</xsl:attribute>
        <xsl:attribute name="border-top">solid .5px #9cc2e5</xsl:attribute>
      </xsl:if>
      <fo:table-cell text-align="left" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="reporter"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="closed"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="invalid"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="opened"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="total"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="open-bugs-by-product">
    <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">Open bugs by Product:</fo:block>
    <fo:block font-size="10pt">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header background-color="#ddebf7" font-weight="bold" border-bottom="solid .5px #9cc2e5" text-align="right">
          <fo:table-cell width="6cm" padding-left=".5em" padding-top=".5em">
            <fo:block text-align="left">Product</fo:block>
          </fo:table-cell>
          <fo:table-cell width="1cm" padding-right=".5em" padding-top=".5em">
            <fo:block>NP</fo:block>
          </fo:table-cell>
          <fo:table-cell width="1cm" padding-right=".5em" padding-top=".5em">
            <fo:block>P1</fo:block>
          </fo:table-cell>
          <fo:table-cell width="1cm" padding-right=".5em" padding-top=".5em">
            <fo:block>P2</fo:block>
          </fo:table-cell>
          <fo:table-cell width="1cm" padding-right=".5em" padding-top=".5em">
            <fo:block>P3</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>Grand Total</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body text-align="right">
          <xsl:apply-templates select="product-bugs"/>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="open-bugs-by-product/product-bugs">
    <fo:table-row>
      <xsl:if test="product = 'Grand Total'">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="background-color">#ddebf7</xsl:attribute>
        <xsl:attribute name="border-top">solid .5px #9cc2e5</xsl:attribute>
      </xsl:if>
      <fo:table-cell text-align="left" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="product"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="np"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="p1"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="p2"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="p3"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="total"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="top-asignees">
    <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">Open bugs, top assignees:</fo:block>
    <fo:block font-size="10pt">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header background-color="#ddebf7" font-weight="bold" border-bottom="solid .5px #9cc2e5" text-align="right">
          <fo:table-cell width="6cm" padding-left=".5em" padding-top=".5em">
            <fo:block text-align="left">Assignee</fo:block>
          </fo:table-cell>
          <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
            <fo:block>Bug Count</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body text-align="right">
          <xsl:apply-templates select="asignee"/>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="top-asignees/asignee">
    <fo:table-row>
      <fo:table-cell text-align="left" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="name"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="count"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="week-summary-report">
    <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">Week #<xsl:value-of select="//report-header/week"/> Summary (<fo:inline color="red">as of <xsl:value-of select="//report-header/date"/></fo:inline>):</fo:block>
    <fo:list-block start-indent="5mm" provisional-distance-between-starts="3mm" provisional-label-separation="2mm">
      <fo:list-item space-before="1.5em">
        <fo:list-item-label end-indent="label-end()"><fo:block>&#x2022;</fo:block></fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block>Production Queue size increased from 62 to <fo:inline color="red">66</fo:inline> bugs; P1/P2 queue is 13 bugs</fo:block>
        </fo:list-item-body>
      </fo:list-item>
      <fo:list-item space-before="1.5em">
        <fo:list-item-label end-indent="label-end()"><fo:block>&#x2022;</fo:block></fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block>Production Bugs Changes made after the previous report (between 2015-09-24 15:00:00 and 2015-10-01 15:00:00)</fo:block>
          <fo:block text-align="center">New bugs (red) vs. Resolved (green)</fo:block>
          <fo:block text-align="center">
            <fo:external-graphic src="url(file:///d:/1.jpg)" content-height="50%" scaling="uniform"/>
          </fo:block>
          <fo:block font-size="9">* Note: 2015-09-26 and 2015-09-27 are weekend days</fo:block>
          <fo:block space-before="1em">New Bugs Created: <fo:inline color="red">35</fo:inline></fo:block>
          <fo:block>Bugs Reopened: <fo:inline color="red">11</fo:inline></fo:block>
          <fo:block>Moved To Queue: <fo:inline color="red">3</fo:inline></fo:block>
          <fo:block>Bugs Resolved: 45</fo:block>
          <fo:block>Total Bugs Updated: 71</fo:block>
          <fo:block>Total Comments: 267</fo:block>
        </fo:list-item-body>
      </fo:list-item>
      <fo:list-item space-before="1.5em">
        <fo:list-item-label end-indent="label-end()"><fo:block>&#x2022;</fo:block></fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block>Bugs Count (Prod Support Bugs, Last 15 Weeks):</fo:block>
          <fo:block>###TABLE</fo:block>
        </fo:list-item-body>
      </fo:list-item>
    </fo:list-block>
  </xsl:template>


</xsl:stylesheet>