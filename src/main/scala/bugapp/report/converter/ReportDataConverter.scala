package bugapp.report.converter

import bugapp.report.model.ReportData

/**
  *
  */
trait ReportDataConverter[To] {
  def convert(value: ReportData): To
}
