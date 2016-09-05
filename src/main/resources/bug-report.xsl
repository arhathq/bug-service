<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
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
            <xsl:apply-templates select="bugs-by-15-weeks"/>
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
        <fo:block>#<xsl:value-of select="format-dateTime(date, '[W]')"/></fo:block>
        <fo:block><xsl:value-of select="format-dateTime(date, '[D1o] [MNn], [Y]', 'en', (), ())"/></fo:block>
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
      <fo:block font-size="8pt" text-align="right">* As of <xsl:value-of select="format-dateTime(//report-header/date, '[FNn], [D]-[MN,*-3]-[Y], [h]:[m01][PN] [z]', 'en', (), ())"/></fo:block>
      <fo:block font-size="8pt" text-align="right">** Excluding CRF Hot Deploys, EComm Hot Deploys, `Dataload Failed`, `New Files Arrived` and `Data Consistency` Reports</fo:block>
    </fo:block>
    <fo:block text-align="center">
      <fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>
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
          <xsl:value-of select="format-dateTime(opened, '[M01]/[D01]/[Y0001] [H01]:[m01]', 'en', (), ())"/>
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

  <xsl:template match="bugs-by-15-weeks">
    <fo:block text-align="center">
      <fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>
    </fo:block>
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
    <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">Week #<xsl:value-of select="//report-header/week"/> Summary (<fo:inline color="red">as of <xsl:value-of select="format-dateTime(//report-header/date, '[FNn], [D1o] [MNn], [h]:[m01] [PN]', 'en', (), ())"/></fo:inline>):</fo:block>
    <fo:list-block start-indent="5mm" provisional-distance-between-starts="3mm" provisional-label-separation="2mm">
      <fo:list-item space-before="1.5em">
        <fo:list-item-label end-indent="label-end()"><fo:block>&#x2022;</fo:block></fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block>Production Queue size <xsl:value-of select="production-queue/state"/> from <xsl:value-of select="production-queue/from"/> to <fo:inline color="red"><xsl:value-of select="production-queue/to"/></fo:inline> bugs; P1/P2 queue is <xsl:value-of select="production-queue/high-priotity-bugs"/> bugs</fo:block>
        </fo:list-item-body>
      </fo:list-item>
      <fo:list-item space-before="1.5em">
        <fo:list-item-label end-indent="label-end()"><fo:block>&#x2022;</fo:block></fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block>Production Bugs Changes made after the previous report (between <xsl:value-of select="format-dateTime(period/from, '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/> and <xsl:value-of select="format-dateTime(period/to, '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/>)</fo:block>
          <fo:block text-align="center">New bugs (red) vs. Resolved (green)</fo:block>
          <fo:block text-align="center">
            <!--<fo:external-graphic src="url('data:image/jpeg;base64,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oooAKKKKACiiigAooooAKKKKAI2/4+E/3W/mKkqNv+PhP91v5ipKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAjj+/N/vf+yipKjj+/N/vf+yipKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqO4/wCPeX/dP8qkqO4/495f90/yoAkooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooArab/AMg+1/65L/IVZqtpv/IPtf8Arkv8hVmgAooooAKKKKACiiigAooooAKKKKACiiigAooooAKjn+4P95f/AEIVJUc/3B/vL/6EKAJKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqNf+Ph/wDdX+ZqSo1/4+H/AN1f5mgCSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKjg+4f95v/QjUlRwfcP8AvN/6EaAJKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigCO4/495f8AdP8AKpKjuP8Aj3l/3T/KpKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAjb/j4T/db+YqSo2/4+E/3W/mKkoAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigCOP783+9/wCyipKjj+/N/vf+yipKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqO4/495f90/yqSo7j/j3l/wB0/wAqAJKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAK2m/8g+1/65L/ACFWarab/wAg+1/65L/IVZoAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACo5/uD/AHl/9CFSVHP9wf7y/wDoQoAkooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACo1/4+H/3V/makqNf+Ph/91f5mgCSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKjg+4f95v8A0I1JUcH3D/vN/wChGgCSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAjuP+PeX/dP8qkqO4/495f90/yqSgAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAI2/4+E/3W/mKkqNv+PhP91v5ipKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAjj+/N/vf+yipKjj+/N/vf+yipKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqO4/wCPeX/dP8qkqO4/495f90/yoAkooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooArab/AMg+1/65L/IVZqtpv/IPtf8Arkv8hVmgAooooAKKKKACiiigAooooAKKKKACiiigAooooAKjn+4P95f/AEIVJUc/3B/vL/6EKAJKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqNf+Ph/wDdX+ZqSo1/4+H/AN1f5mgCSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKjg+4f95v/QjUlRwfcP8AvN/6EaAJKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigCO4/495f8AdP8AKpKjuP8Aj3l/3T/KpKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAjb/j4T/db+YqSo2/4+E/3W/mKkoAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigCOP783+9/wCyipKjj+/N/vf+yipKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAqO4/495f90/yqSo7j/j3l/wB0/wAqAJKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAK2m/8g+1/65L/ACFWarab/wAg+1/65L/IVZoAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACo5/uD/AHl/9CFSVHP9wf7y/wDoQoAkooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACo1/4+H/3V/makqNf+Ph/91f5mgCSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKjg+4f95v8A0I1JUcH3D/vN/wChGgCSiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAjuP+PeX/dP8q8t+MH7Rvh74L61pml6tpWuapd6hC88KaPa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')" content-height="50%" scaling="uniform"/>-->
            <fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>
          </fo:block>
          <fo:block font-size="9">* Note: <xsl:value-of select="period/weekend1"/> and <xsl:value-of select="period/weekend2"/> are weekend days</fo:block>
          <fo:block space-before="1em">New Bugs Created: <fo:inline color="red"><xsl:value-of select="statistics/new"/></fo:inline></fo:block>
          <fo:block>Bugs Reopened: <fo:inline color="red"><xsl:value-of select="statistics/reopened"/></fo:inline></fo:block>
          <fo:block>Moved To Queue: <fo:inline color="red"><xsl:value-of select="statistics/moved"/></fo:inline></fo:block>
          <fo:block>Bugs Resolved: <xsl:value-of select="statistics/resolved"/></fo:block>
          <fo:block>Total Bugs Updated: <xsl:value-of select="statistics/bugs-updated"/></fo:block>
          <fo:block>Total Comments: <xsl:value-of select="statistics/total-comments"/></fo:block>
        </fo:list-item-body>
      </fo:list-item>
      <fo:list-item space-before="1.5em">
        <fo:list-item-label end-indent="label-end()"><fo:block>&#x2022;</fo:block></fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <fo:block space-after=".5em">Bugs Count (Prod Support Bugs, Last <xsl:value-of select="bugs-count/period"/> Weeks):</fo:block>
        </fo:list-item-body>
      </fo:list-item>
    </fo:list-block>
    <fo:block font-size="10pt" padding-left="1.em">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header font-weight="bold" border-bottom="solid .5px #9cc2e5" text-align="right">
          <fo:table-cell width="4cm" padding-top=".5em">
            <fo:block text-align="left"></fo:block>
          </fo:table-cell>
          <fo:table-cell width="2cm" border="solid .5px black" padding-right=".5em" padding-top=".5em">
            <fo:block>INVALID</fo:block>
          </fo:table-cell>
          <fo:table-cell width="2cm" border="solid .5px black" padding-right=".5em" padding-top=".5em">
            <fo:block>CLOSED</fo:block>
          </fo:table-cell>
          <fo:table-cell width="2cm" border="solid .5px black" padding-right=".5em" padding-top=".5em">
            <fo:block>OPEN</fo:block>
          </fo:table-cell>
          <fo:table-cell width="2cm" border="solid .5px black" padding-right=".5em" padding-top=".5em">
            <fo:block>TOTAL</fo:block>
          </fo:table-cell>
        </fo:table-header>
        <fo:table-body text-align="right">
          <xsl:apply-templates select="bugs-count/table/row"/>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="week-summary-report/bugs-count/table/row">
    <fo:table-row>
      <fo:table-cell border="solid .5px black" text-align="left" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:choose>
            <xsl:when test="line castable as xs:date">
              <xsl:value-of select="format-date(line, 'Week [Y]-[W] [FNn,*-3]')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="line"/>
            </xsl:otherwise>
          </xsl:choose>

        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="invalid"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="closed"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="open"/>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="total"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

</xsl:stylesheet>