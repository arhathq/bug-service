<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
  <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>
  <xsl:param name="versionParam" select="'1.0'"/>

  <xsl:template match="bug-reports">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
      <fo:layout-master-set>
        <fo:simple-page-master master-name="simpleA4" page-height="29.7cm" page-width="21cm" margin-top="1cm" margin-bottom="1cm" margin-left="1.5cm" margin-right=".5cm">
          <fo:region-body region-name="xsl-region-body" margin="0.5cm" margin-top="3cm"/>
          <fo:region-before region-name="xsl-region-before" margin="0.5cm" extent="3cm" display-align="before" />
          <fo:region-after region-name="xsl-region-after" margin="0.5cm" display-align="after" extent="3cm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="simpleA4">
        <xsl:apply-templates select="report-header"/>
        <xsl:apply-templates select="report-footer"/>
        <fo:flow flow-name="xsl-region-body">
          <fo:block/>
          <xsl:apply-templates select="all-open-bugs"/>
          <xsl:apply-templates select="all-open-bugs-chart"/>
          <xsl:apply-templates select="open-bugs"/>
          <xsl:apply-templates select="bugs-by-weeks-15"/>
        </fo:flow>
      </fo:page-sequence>

      <fo:page-sequence master-reference="simpleA4">
        <xsl:apply-templates select="report-header"/>
        <xsl:apply-templates select="report-footer"/>
        <fo:flow flow-name="xsl-region-body">
          <fo:block/>
          <xsl:apply-templates select="reporter-bugs-by-weeks-15"/>
          <xsl:apply-templates select="reporter-bugs-by-weeks-1"/>
          <xsl:apply-templates select="priority-bugs-by-this-week"/>
          <xsl:apply-templates select="open-bugs-by-product"/>
          <xsl:apply-templates select="top-asignees"/>
        </fo:flow>
      </fo:page-sequence>

      <fo:page-sequence master-reference="simpleA4">
        <xsl:apply-templates select="report-header"/>
        <xsl:apply-templates select="report-footer"/>
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

  <xsl:template match="report-footer">
    <fo:static-content flow-name="xsl-region-after">
      <fo:block font-size="8pt" font-style="italic" text-align="center" space-after="5mm" color="black">
        <fo:block><xsl:value-of select="note"/></fo:block>
      </fo:block>
    </fo:static-content>
  </xsl:template>

  <xsl:template match="all-open-bugs">
    <fo:block font-size="12pt" font-weight="bold">All Open Production Bugs *</fo:block>
    <fo:block font-size="10pt">
      <fo:table width="100%" border-collapse="collapse">
        <fo:table-header color="white" background-color="#5b9bd5">
          <fo:table-cell border="solid .5px black" padding="1em" width="4cm">
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
      <fo:block font-size="8pt" text-align="right" space-before="1em">* As of <xsl:value-of select="format-dateTime(//report-header/date, '[FNn], [D]-[MN,*-3]-[Y], [h]:[m01][PN] [z]', 'en', (), ())"/></fo:block>
      <xsl:if test="excludedComponents != ''">
        <fo:block font-size="8pt" text-align="right">** Excluding <xsl:value-of select="excludedComponents"/> Reports</fo:block>
      </xsl:if>
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

  <xsl:template match="all-open-bugs-chart">
    <fo:block text-align="center" margin-top="1cm">
      <fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>
    </fo:block>
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
          <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="9cm">
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

  <xsl:template match="bugs-by-weeks-15">
    <fo:block>
        <fo:block text-align="center" keep-with-next.within-page="always">
            <fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>
        </fo:block>
        <fo:block font-size="6pt" space-before="0" margin-top="0" keep-with-next.within-page="always">
            <fo:table width="100%" border-collapse="collapse">
                <fo:table-header border="solid .5px black" text-align="center">
                    <fo:table-cell border="solid .5px black" width="1cm" padding-left=".5em" padding-top=".5em">
                        <fo:block/>
                    </fo:table-cell>
                    <xsl:for-each select="weekly-bugs/header/*">
                        <fo:table-cell border="solid .5px black" width=".99cm" padding-left=".5em" padding-top=".5em">
                            <fo:block text-align="left"><xsl:value-of select="."/></fo:block>
                        </fo:table-cell>
                    </xsl:for-each>
                </fo:table-header>
                <fo:table-body text-align="center">
                    <xsl:apply-templates select="weekly-bugs"/>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </fo:block>
  </xsl:template>

  <xsl:template match="bugs-by-weeks-15/weekly-bugs">
    <xsl:for-each select="row">
      <fo:table-row>
        <fo:table-cell text-align="left" border="solid .5px black" padding-left=".5em" padding-top=".5em">
          <fo:block>
            <xsl:value-of select="name"/>
          </fo:block>
        </fo:table-cell>
        <xsl:for-each select="./*">
          <xsl:if test="name() = 'value'">
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
              <fo:block>
                <xsl:value-of select="."/>
              </fo:block>
            </fo:table-cell>
          </xsl:if>
        </xsl:for-each>
      </fo:table-row>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="reporter-bugs-by-weeks-15">
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

  <xsl:template match="reporter-bugs-by-weeks-15/reporter-bugs">
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

  <xsl:template match="reporter-bugs-by-weeks-1">
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

  <xsl:template match="reporter-bugs-by-weeks-1/reporter-bugs">
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
            <fo:block text-align="left">Priority</fo:block>
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
      <xsl:if test="priority = 'Grand Total'">
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="background-color">#ddebf7</xsl:attribute>
        <xsl:attribute name="border-top">solid .5px #9cc2e5</xsl:attribute>
      </xsl:if>
      <fo:table-cell text-align="left" padding-left=".5em" padding-top=".5em">
        <fo:block>
          <xsl:value-of select="priority"/>
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
          <xsl:if test="production-queue/blocked-bugs!=''">
            <fo:block margin-left="1.5em">- <xsl:value-of select="production-queue/blocked-bugs"/> bugs are BLOCKED</fo:block>
          </xsl:if>
        </fo:list-item-body>
      </fo:list-item>
      <fo:list-item space-before="1.5em">
        <fo:list-item-label end-indent="label-end()"><fo:block>&#x2022;</fo:block></fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
          <xsl:apply-templates select="//week-summary-report-chart"/>
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

  <xsl:template match="week-summary-report-chart">
    <fo:block>Production Bugs Changes made after the previous report (between <xsl:value-of select="format-dateTime(period/from, '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/> and <xsl:value-of select="format-dateTime(period/to, '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/>)</fo:block>
    <fo:block text-align="center">New bugs (red) vs. Resolved (green)</fo:block>
    <fo:block text-align="center">
      <fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>
    </fo:block>
    <fo:block font-size="9">* Note: <xsl:value-of select="format-dateTime(period/weekend1, '[Y0001]-[M01]-[D01]')"/> and <xsl:value-of select="format-dateTime(period/weekend2, '[Y0001]-[M01]-[D01]')"/> are weekend days</fo:block>
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
          <xsl:choose>
            <xsl:when test="line = 'Average'">
              <xsl:value-of select="format-number(invalid, '#.00')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="format-number(invalid, '0')"/>
            </xsl:otherwise>
          </xsl:choose>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:choose>
            <xsl:when test="line = 'Average'">
              <xsl:value-of select="format-number(closed, '#.00')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="format-number(closed, '0')"/>
            </xsl:otherwise>
          </xsl:choose>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:choose>
            <xsl:when test="line = 'Average'">
              <xsl:value-of select="format-number(open, '#.00')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="format-number(open, '0')"/>
            </xsl:otherwise>
          </xsl:choose>
        </fo:block>
      </fo:table-cell>
      <fo:table-cell border="solid .5px black" padding-right=".5em" padding-top=".5em">
        <fo:block>
          <xsl:choose>
            <xsl:when test="line = 'Average'">
              <xsl:value-of select="format-number(total, '#.00')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="format-number(total, '0')"/>
            </xsl:otherwise>
          </xsl:choose>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
  </xsl:template>

</xsl:stylesheet>