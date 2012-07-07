package memorysystem;

class StrTok {
  private String remainder;
  private String delimiters;

  public StrTok(String s, String delims)
  {
    remainder = s;
    delimiters = delims;
  }

  public String next(String delims)
  {
    if (delims != null) {
      delimiters = delims;
    }
    int i = 0;
    while (i < remainder.length() && delimiters.indexOf(remainder.charAt(i)) >= 0) {
      i++;
    }
    if (i >= remainder.length()) {
      return null;
    }
    remainder = remainder.substring(i);
    while (i < remainder.length() && delimiters.indexOf(remainder.charAt(i)) == -1) {
      i++;
    }
    String r = remainder.substring(0, i);
    if (i < remainder.length()) {
      i++;
    }
    remainder = remainder.substring(i);
    return r;
  }

  public String next()
  {
    return next(null);
  }
}