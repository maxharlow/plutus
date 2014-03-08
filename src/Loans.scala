import java.io.File
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.anormcypher.Cypher
import com.github.tototoshi.csv.CSVReader
import CypherTools._

object Loans {

  implicit class CypherParameterValue(v: String) {
    val value = v.trim.filter(_ >= ' ').replace("""\""", """\\""").replace("'", """\'""")
    def int = Option(value.replaceAll("[^0-9]", "").dropRight(2)).filter(_.nonEmpty) // inexplicably values here are given to four decimal places
    def string = if (value.isEmpty) None else Some("'" + value + "'")
    def boolean = Some((!value.isEmpty).toString)
    def date = {
      val format = DateTimeFormat.forPattern("dd/MM/yyyy")
      if (value.isEmpty) None
      else Some(DateTime.parse(value, format).toString("yyyyMMdd"))
    }
  }

  def run() {
    val loans = CSVReader.open(new File(Config.loansData)).allWithHeaders
    loans foreach { entry =>
      // benefactor
      val benefactorName = entry("Lender name").string
      val benefactorCompanyNumber = entry("Company reg. no.").replaceAll("[^0+A-Za-z0-9]", "").replaceAll("^0*", "").string // optional
      val benefactor = Map(
        "name" -> benefactorName,
        "type" -> entry("Lender type").string,
        "companyNumber" -> benefactorCompanyNumber,
        "postcode" -> entry("Postcode").string // optional
      )
      val benefactorProperties = benefactor.propertise()
      val benefactorResult = Cypher(s"MERGE (:Benefactor {$benefactorProperties})").execute()
      if (!benefactorResult) println(" => failed to add benefactor")

      // recipient
      val recipientName = entry("Entity name").string
      val recipient = Map(
        "name" -> recipientName,
        "type" -> entry("Entity type").string
      )
      val recipientProperties = recipient.propertise()
      val recipientResult = Cypher(s"MERGE (:Recipient {$recipientProperties})").execute()
      if (!recipientResult) println(" => failed to add recipient")

      // loan
      val loan = Map(
        "ecReference" -> entry("EC reference").string,
        "type" -> entry("Type of borrowing").string,
        "value" -> entry("Total amount").int, // in pence
        "referenceNumber" -> entry("Loan reference no.").string, // optional
        "rate" -> entry("Rate").string, // optional
        "status" -> entry("Status").string,
        "amountRepaid" -> entry("Amount repaid").int,
        "amountConverted" -> entry("Amount converted").int,
        "amountOutstanding" -> entry("Amount outstanding").int,
        "startDate" -> entry("Start date").date,
        "endDate" -> entry("End date").date, // optional
        "repaidDate" -> entry("Date repaid").date, // optional
        "ecLastNotifiedDate" -> entry("Date EC last notified").date,
        "recordedBy" -> entry("Rec'd by (AU)").string, // optional
        "complianceBreach" -> entry("Compliance breach").string
      )
      val loanProperties = loan.propertise()
      val loanMatchCypher = s"MATCH (b:Benefactor {name:${benefactorName.get}}), (r:Recipient {name:${recipientName.get}})"
      val loanCreateCypher = s"CREATE (b)-[:LOANED {$loanProperties}]->(r)"
      val loanResult = Cypher(s"$loanMatchCypher $loanCreateCypher").execute()
      if (!loanResult) println(" => failed to add loan")

      println(s"Adding loan: ${benefactorName.get} -> ${recipientName.get}")
    }
  }

}
