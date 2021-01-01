package cook
import scala.collection.mutable.ArrayBuffer
import java.io._
import scala.io.Source._
import scala.util.control.Breaks._
import java.awt.Color.RED
import javax.swing.{ ImageIcon, BorderFactory }

class FileProcessor(ui: UI) {
  var menu = ui.menu
  var fridge = ui.fridge
  /**IO Write Function*/
  def IOWrite() = {
    var file = new File("src/main/scala/saved_data/data.txt")
    val pw = new PrintWriter(file)
    pw.write("RecipeBook v0.1 data\n")
    pw.write("name \t ingredients \t main_unit \t second_unit \t density \t tag \t description \t isMenu \t amount\n")
    for ((food, num) <- fridge.foodList) {
      var boo = if (food.isMenu) "1" else "0"
      var ingredientsString = ""
      if (!food.hasNoIngredients) {
        ingredientsString = food.ingredients.toList.map(x => x._1.name + "=" + x._2.toString).mkString(",")
      }
      pw.write(food.name + "\t" + ingredientsString + "\t" + food.main_unit + "\t" + food.second_unit + "\t" + food.density.toString + "\t" + food.tag + "\t" + food.description + "\t" + boo + "\t" + num.toString + "\n")
    }
    pw.close
  }

  /**IO Read Function*/
  def IOReadlines(): Iterator[String] = {
    var lines = fromFile("src/main/scala/saved_data/default.txt").getLines.filter(_.nonEmpty)
    try {
      lines = fromFile("src/main/scala/saved_data/data.txt").getLines.filter(_.nonEmpty)
      ui.leftFeedback.text = "> User-saved file loaded successfully. "
    } catch {
      case e: FileNotFoundException => ui.leftFeedback.text = "> User-saved file not found. Loaded from default. "
    } finally {
      ui.leftFeedback.repaint()
      if (!lines.next().startsWith("RecipeBook")) throw new IOException
      var temp = lines.next() // Read header
    }
    lines
  }

  def lineProcessor(line: String, container: ArrayBuffer[Array[Any]]): Unit = {
    var splitted = line.split("\t").map(_.trim)
    if (splitted.size != 9) throw new IOException
    var name_add: String = splitted(0)
    var ingredients_add: String = splitted(1)
    var first_unit_add: String = splitted(2).toLowerCase
    var second_unit_add: String = splitted(3).toLowerCase
    var density_add: Double = splitted(4).toDouble
    var alleriges_add: String = splitted(5).toUpperCase
    var description_add: String = splitted(6)
    var isMenu_add: Boolean = if (splitted(7) == "1") true else if (splitted(7) == "0") false else throw new IOException
    var amount_add: Double = splitted(8).toDouble
    if (amount_add > 1000) {
      amount_add = 1000
      println("Notice: The maximum amount allowed in this system is 1000. Your input has been changed to 1000.")
    } else if (amount_add < 0) {
      amount_add = 0
      println("Notice: The amount cannot be negative. Your input has been changed to 0.")
    }
    if (density_add < 0) {
      density_add = 0
      println("Notice: Density cannot be negative. System changed it to the default value: 0")
    }
    if (ingredients_add.isEmpty) {
      var food_add = Food(name_add, scala.collection.mutable.Map[Food, Double](), first_unit_add, second_unit_add, density_add, alleriges_add, description_add)
      if (isMenu_add) food_add.setToMenu()
      fridge.addFood(food_add, amount_add)
    } else {
      var ingre_map: Map[String, Double] = {
        var item_list = ingredients_add.split(",")
        var name_list = ArrayBuffer[String]()
        var amount_list = ArrayBuffer[Double]()
        for (item <- item_list) {
          name_list += item.split("=").head.trim
          var temp_amount = item.split("=").last.toDouble
          if (temp_amount > 1000) {
            temp_amount = 1000
            println("Notice: The maximum amount allowed in this system is 1000. The amount of " + item.split("=").head.trim + "has been changed to 1000.")
          } else if (temp_amount < 0) {
            temp_amount = 0
            println("Notice: The amount cannot be negative. The amount of " + item.split("=").head.trim + "has been changed to 0.")
          }
          amount_list += temp_amount
        }
        (name_list zip amount_list).toMap
      }
      container += Array[Any](name_add, ingre_map, first_unit_add, second_unit_add, density_add, alleriges_add, description_add, isMenu_add, amount_add)
    }
  }

  def loadFromIO() = {
    var lines = IOReadlines()
    try {
      var buffer = ArrayBuffer[Array[Any]]()
      for (line <- lines) lineProcessor(line, buffer)
      var accumulator = 0
      var threshold = 100
      while (buffer.nonEmpty) {
        try {
          for (data <- buffer) {
            var name = data(0).asInstanceOf[String]
            var ingredients = data(1).asInstanceOf[Map[String, Double]]
            var firstUnit = data(2).asInstanceOf[String]
            var secondUnit = data(3).asInstanceOf[String]
            var density = data(4).asInstanceOf[Double]
            var allergies = data(5).asInstanceOf[String]
            var description = data(6).asInstanceOf[String]
            var isMenu = data(7).asInstanceOf[Boolean]
            var amount = data(8).asInstanceOf[Double]
            var ingredgentNames = ingredients.map(_._1)
            if (menu.allIngredientsExist(ingredgentNames)) {
              var ingre_mapped = ingredients.map(x => (menu.returnFoodWithName(x._1).get, x._2))
              var ingre = collection.mutable.Map(ingre_mapped.toSeq: _*)
              //var tag_ingre = (ingre.keys.map(_.tag).mkString("").toUpperCase+data.aller).distinct
              var foodToBeAdd = Food(name, ingre, firstUnit, secondUnit, density, allergies, description)
              if (isMenu) foodToBeAdd.setToMenu()
              fridge.addFood(foodToBeAdd, amount)
              buffer -= data
            }
          }
        } catch {
          case e: NullPointerException => ui.p("Notice: One food is waiting for raw material(s)")
        }
        accumulator += 1
        if (accumulator > threshold) throw new IOException
      }
      ui.p("Notice: User Interface loaded successfully")
      ui.p("Notice: " + menu.menuFoodlist.size.toString + " menus and " + menu.nonMenuFoodlist.size.toString + " ingredients have been imported")
    } catch {
      case e: IOException => {
        ui.leftFeedback.text = "> Errors in file. Please check your input file. "
        ui.leftFeedback.foreground = RED
        ui.buttonSave = ui.buttonExit
        ui.searchButton.enabled = false
        ui.backButton.enabled = false
        ui.leftMultifunctionalText.repaint()
        ui.searchButton.repaint()
        ui.leftFeedback.repaint()
        ui.buttonSave.repaint()
      }
    }
  }

}