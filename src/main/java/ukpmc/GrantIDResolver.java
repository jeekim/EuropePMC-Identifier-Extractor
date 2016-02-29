package ukpmc;

import ukpmc.scala.IDResolver;

public class GrantIDResolver implements IDResolver {

   public boolean isValidID(String domain, String id) {
      return false;
   }

}
