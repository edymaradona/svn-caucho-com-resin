package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import javax.ejb.Query;

import com.caucho.ejb.entity2.EntityManagerProxy;

/**
 * A client to illustrate the query.
 */
public class OneToManyServlet extends HttpServlet {

  private EntityManagerProxy _entityManager;

  /**
   * Sets the entity manager.
   */
  public void setEntityManager(EntityManagerProxy entityManager)
  {
    _entityManager = entityManager;
  }

  public void init()
  {
    House house = null;
      
    try {
      house = (House) _entityManager.find("House", new Long(1));
    } catch (Throwable e) {
    }

    if (house == null) {
      House gryffindor = new House("Gryffindor");
      _entityManager.create(gryffindor);
	
      House slytherin = new House("Slytherin");
      _entityManager.create(slytherin);
	
      House ravenclaw = new House("Ravenclaw");
      _entityManager.create(ravenclaw);
	
      House hufflepuff = new House("Hufflepuff");
      _entityManager.create(hufflepuff);

      Student student;

      student = new Student("Harry Potter", "M", gryffindor);
      _entityManager.create(student);

      student = new Student("Ron Weasley", "M", gryffindor);
      _entityManager.create(student);

      student = new Student("Hermione Granger", "F", gryffindor);
      _entityManager.create(student);

      student = new Student("Draco Malfoy", "M", slytherin);
      _entityManager.create(student);

      student = new Student("Millicent Bulstrode", "F", slytherin);
      _entityManager.create(student);

      student = new Student("Penelope Clearwater", "F", ravenclaw);
      _entityManager.create(student);
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    String sql = "SELECT h FROM House h";
    
    Query allHouse = _entityManager.createQuery("SELECT o FROM House o");

    List houses = allHouse.listResults();

    for (int i = 0; i < houses.size(); i++) {
      House house = (House) houses.get(i);

      out.println("<h3>" + house.getName() + "</h3>");

      for ( Student student : house.getStudents() ) {
        out.println( student.getName() + "<br>" );
      }
    }
  }
}
