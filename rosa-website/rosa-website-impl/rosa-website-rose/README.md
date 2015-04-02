##Adding new page
A new page in this site needs several classes created before it can be added to the web application.

 * Activity (implements Activity, View.Presenter if applicable) - 
 * Place (extends Place) - new Place contains state information for the app
   * Tokenizer (implements PlaceTokenizer<New_Place>) - easiest to make this a static inner class of the Place
 * View interface (extends IsWidget) - 
 * View implementation (implements View interface) - UI
 
Along with new classes, some changes need to be made to existing places in the code.

 * Activity must be added to the ActivityMapper (RosaActivityMapper)
 * View must be added to the ClientFactory
 * HistoryMapper should be modified if a custom history token scheme is used, otherwise add the Place.Tokenizer to the
   default mapper.