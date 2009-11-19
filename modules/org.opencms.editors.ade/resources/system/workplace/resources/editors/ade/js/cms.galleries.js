(function(cms) {

   /** html-id for tabs. */
   var idTabs = cms.galleries.idTabs = 'cms-gallery-tabs';
   
   /** html-id for the tab with search results. */
   var idTabResult = cms.galleries.idTabResult = 'tabs-result';
   
   /** html-class for the inner of the scrolled list with items. */
   var classScrollingInner = cms.galleries.classScrollingInner = 'cms-list-scrolling-innner';
   
   /** html-class for hovered list item elements. */
   var classListItemHover = cms.galleries.classListItemHover = 'cms-list-item-hover';
   
   /** html-class for active list item elements. */
   var classListItemActive = cms.galleries.classListItemActive = 'cms-list-item-active';
   
   /** html-class for the item title in the list. */
   var classListItemTitle = cms.galleries.classListItemTitle = 'cms-list-title';
   
   /** html-class for the panel above or under the list of items. */
   var classListOptions = cms.galleries.classListOptions = 'cms-list-options';
   
   /** html-class fragment for level information of the categories. */
   var classConstLevel = 'cms-level-';
   
   /** Array with available search criteria. */
   var keysSearchObject = cms.galleries.keysSearchObject = ['types', 'galleries', 'categories', 'query'];
   
   var clickTimer;
   
   /** Map of key words for the criteria buttons on the result tab. */
   var criteriaStr = cms.galleries.criteriaStr = {
      types: ['Type: ', 'Types: '],
      galleries: ['Gallery: ', 'Galleries: '],
      categories: ['Category: ', 'Categories: '],
      query: ['Search query: ', 'Seach queries: ']
   
   };
   
   
   
   /** html fragment for the tab with the results of the search. */
   var htmlTabResultSceleton = cms.galleries.htmlTabResultSceleton = '<div id="' + cms.galleries.idTabResult + '">\
            <div class="cms-result-criteria"></div>\
            <div id="resultoptions" class="ui-widget ' +
   cms.galleries.classListOptions +
   '">\
                        <span class="cms-drop-down">\
                            <label>Sort by:&nbsp;</label>\
                        </span>\
             </div>\
             <div id="results" class="cms-list-scrolling ui-corner-all result-tab-scrolling">\
                        <ul class="cms-list-scrolling-innner"></ul>\
             </div>\
             <div class="result-pagination"></div>\
         </div>';
   
   /** html fragment for the tab with the types' list. */
   var htmlTabTypesSceleton = cms.galleries.htmlTabTypesSceleton = '<div id="tabs-types">\
                <div id="typesoptions" class="ui-widget ' + cms.galleries.classListOptions + '">\
                    <span class="cms-drop-down">\
                        <label>Sort by:</label>\
                    </span>\
                    <span class="cms-ft-search"><label>Search:</label><input type="text" class="ui-corner-all ui-widget-content" /></span>\
                </div>\
                <div id="types" class="cms-list-scrolling ui-corner-all criteria-tab-scrolling">\
                    <ul class="cms-list-scrolling-innner"></ul>\
                </div>\
              </div>';
   
   /** html fragment for the tab with the galleries' list. */
   var htmlTabGalleriesSceleton = cms.galleries.htmlTabGalleriesSceleton = '<div id="tabs-galleries">\
                <div id="galleriesoptions" class="ui-widget ' + cms.galleries.classListOptions + '">\
                    <span class="cms-drop-down">\
                        <label>Sort by:</label>\
                    </span>\
                    <span class="cms-ft-search"><label>Search:</label><input type="text" class="ui-corner-all ui-widget-content" /></span>\
                </div>\
                <div id="galleries" class="cms-list-scrolling ui-corner-all criteria-tab-scrolling">\
                    <ul class="cms-list-scrolling-innner"></ul>\
                </div>\
              </div>';
   
   /** html fragment for the tab with the categories' list. */
   var htmlTabCategoriesSceleton = cms.galleries.htmlTabCategoriesSceleton = '<div id="tabs-categories">\
                <div id="categoriesoptions" class="ui-widget ' + cms.galleries.classListOptions + '">\
                    <span class="cms-drop-down">\
                        <label>Sort by:</label>\
                    </span>\
                    <span class="cms-ft-search"><label>Search:</label><input type="text" class="ui-corner-all ui-widget-content" /></span>\
                </div>\
                <div id="categories" class="cms-list-scrolling ui-corner-all criteria-tab-scrolling">\
                    <ul class="cms-list-scrolling-innner"></ul>\
                </div>\
              </div>';
   
   /** html fragment for the tab with the types' list. */
   var htmlTabFTSeachSceleton = cms.galleries.htmlTabFTSeachSceleton = '<div id="tabs-fulltextsearch">\
             <div class="cms-search-panel ui-corner-all">\
                    <div class="ui-widget cms-search-options">\
                        <span id="searchQuery" class="cms-item-left"><label>Search for:</label><input type="text" class="ui-corner-all ui-widget-content" /></span>\
                    </div>\
                    <div class="ui-widget cms-search-options">\
                        <div class="cms-item-left">Search in:</div>\
                        <div id="searchInTitle" class="cms-list-checkbox"></div>\
                        <div class="cms-checkbox-label">Title</div>\
                        <div id="searchInContent" class="cms-list-checkbox"></div>\
                        <div class="cms-checkbox-label">Content</div>\
                    </div>\
                    <div class="ui-widget cms-search-options">\
                        <span id="searchBefore" class="cms-item-left cms-input-date"><label>Changed after:</label><input type="text" class="ui-corner-all ui-widget-content" /></span>\
                        <span id="searchBefore" class="cms-item-left cms-input-date"><label>Changed before:</label><input type="text" class="ui-corner-all ui-widget-content" /></span>\
                    </div>\
                    <div class="cms-search-options">\
                        <button class="ui-state-default ui-corner-all cms-item-left-bottom">Search</button>\
                    </div>\
             </div>\
          </div>';
   
   /** html fragment for the <li> in the galleries list. */
   /*var listGalleryElement = cms.galleries.listGalleryElement = function(itemTitle, itemUrl, itemIcon) {
   
      return $('<li></li>').addClass('cms-list').addClass('cms-list-with-checkbox').attr('rel', itemUrl).append('<div class="cms-list-checkbox"></div>\
                         <div class="cms-list-item ui-widget-content ui-state-default ui-corner-all">\
                             <div class="cms-list-image" style="background-image: url(' + itemIcon + ');"></div>\
                             <div class="cms-list-itemcontent">\
                                 <div class="' + cms.galleries.classListItemTitle + '">' + itemTitle + '</div>\
                                 <div class="cms-list-url">' +
      itemUrl +
      '</div>\
                             </div>\
                         </div>');
   }*/
   
   /** html fragment for the <li> in the types list. */
   /*var listTypeElement = cms.galleries.listTypeElement = function(itemTitle, itemId, itemDesc, itemIcon) {
   
      return $('<li></li>').addClass('cms-list').addClass('cms-list-with-checkbox').attr('rel', itemId).append('<div class="cms-list-checkbox"></div>\
                         <div class="cms-list-item ui-widget-content ui-state-default ui-corner-all">\
                             <div class="cms-list-image" style="background-image: url(' + itemIcon + ');"></div>\
                             <div class="cms-list-itemcontent">\
                                 <div class="' + cms.galleries.classListItemTitle + '">' + itemTitle + '</div>\
                                 <div class="cms-list-url">' +
      itemDesc +
      '</div>\
                             </div>\
                         </div>');
   }*/
   
   /** html fragment for the <li> in the categories list. */
  /* var listCategoryElement = cms.galleries.listCategoryElement = function(itemTitle, itemUrl, itemLevel, classItemActive) {
   
      return $('<li></li>').addClass('cms-list ' + classItemActive + ' ' + classConstLevel + itemLevel).addClass('cms-list-with-checkbox').attr('rel', itemUrl).append('<div class="cms-list-checkbox"></div>\
                         <div class="cms-list-item ui-widget-content ui-state-default ui-corner-all">\
                             <div class="cms-list-image"></div>\
                             <div class="cms-list-itemcontent">\
                                 <div class="' + cms.galleries.classListItemTitle + '">' + itemTitle + '</div>\
                                 <div class="cms-list-url">' +
      itemUrl +
      '</div>\
                             </div>\
                         </div>');
   }
   
   var listResultElemenAlt = cms.galleries.listResultElementAlt = function(itemTitle, itemPath, itemIcon) {
      return $('<li class="cms-result-list-item"></li>').attr('rel', itemPath).append('<div class="ui-widget-content ui-state-default ui-corner-all">\
                             <div class="cms-list-image" style="background-image: url(' + itemIcon + ');"></div>\
                             <div>\
                                 <div class="cms-result-list-title">' + itemTitle + '</div>\
                                 <div class="cms-result-list-path">' +
      itemPath +
      '</div>\
                             </div>\
                         </div>');
   }*/
   
 /* var listResultElement = cms.galleries.listResultElement = function(itemTitle, itemPath, itemIcon) {
      return $('<li class="cms-list"></li>').attr('rel', itemPath).append('<div class="cms-list-item ui-widget-content ui-state-default ui-corner-all">\
                             <div class="cms-list-image" style="background-image: url(' + itemIcon + ');"></div>\
                             <div class="cms-list-itemcontent">\
                                 <div class="cms-list-title">' + itemTitle + '</div>\
                                 <div class="cms-list-url">' +
      itemPath +
      '</div>\
                             </div>\
                         </div>');
   }*/
   
   /**
    * Map of selected search criteria.
    *
    * types: array of resource ids for the available resource types
    * galleries: array of paths to the available galleries
    * categories: array of paths to the available categories
    * searchquery: the search key word
    * page: the page number of the requested result page
    * isChanged: map of flags indicating if one of the search criteria is changed and should be taken into account
    */
   var searchObject = cms.galleries.searchObject = {
      types: [],
      galleries: [],
      categories: [],
      query: '',
      page: 1,
      searchfields: '',
      matchesperpage: 8,
      isChanged: {
         types: false,
         galleries: false,
         categories: false,
         query: false
      }
   };
   
   /** Saves the initial list of all available search criteria from server. */
   var searchCriteriaListsAsJSON = cms.galleries.searchCriteriaListsAsJSON = {};
   
   /**
    * Dummy content
    */
   var dummyGalleries = cms.galleries.dummyGalleries = [{
      title: 'Gallery 1',
      path: 'url/to/gallery1/',
      icon: '../../filetypes/downloadgallery.gif'
   }, {
      title: 'Gallery 2',
      path: 'url/to/gallery2/',
      icon: '../../filetypes/downloadgallery.gif'
   }, {
      title: 'Gallery 3',
      path: 'url/to/gallery3/',
      icon: '../../filetypes/downloadgallery.gif'
   }, {
      title: 'Gallery 4',
      path: 'url/to/gallery4/',
      icon: '../../filetypes/imagegallery.gif'
   }, {
      title: 'Gallery 5',
      path: 'url/to/gallery5/',
      icon: '../../filetypes/imagegallery.gif'
   }, {
      title: 'Gallery 6',
      path: 'url/to/gallery6/',
      icon: '../../filetypes/imagegallery.gif'
   }, {
      title: 'Gallery 7',
      path: 'url/to/gallery7/',
      icon: '../../filetypes/imagegallery.gif'
   }, {
      title: 'Gallery 8',
      path: 'url/to/gallery8/',
      icon: '../../filetypes/imagegallery.gif'
   }];
   
   /**
    * Dummy content
    */
   var dummyCategories = cms.galleries.dummyCategories = [{
      title: 'Category 1',
      path: 'url/to/Category1/',
      icon: '../../filetypes/folder.gif',
      level: 0
   }, {
      title: 'Category 2',
      path: 'url/to/Category2/',
      icon: '../../filetypes/folder.gif',
      level: 1
   }, {
      title: 'Category 3',
      path: 'url/to/Category3/',
      icon: '../../filetypes/folder.gif',
      level: 2
   }, {
      title: 'Category 4',
      path: 'url/to/Category4/',
      icon: '../../filetypes/folder.gif',
      level: 1
   }, {
      title: 'Category 5',
      path: 'url/to/Category5/',
      icon: '../../filetypes/folder.gif',
      level: 0
   }, {
      title: 'Category 6',
      path: 'url/to/Category6/',
      icon: '../../filetypes/folder.gif',
      level: 1
   }, {
      title: 'Category 7',
      path: 'url/to/Category7/',
      icon: '../../filetypes/folder.gif',
      level: 2
   }, {
      title: 'Category 8',
      path: 'url/to/gallery8/',
      icon: '../../filetypes/folder.gif',
      level: 3
   }];
   
   /** Dummy Array with availabe types for galleries, should be configurable. */
   var dummyTypes = cms.galleries.dummyTypes = [8, 9, 10, 11, 12];
   
   /**
    * Init function for search/add dialog.
    */
   var initAddDialog = cms.galleries.initAddDialog = function() {
      // init tabs for add dialog
      var resultTab = $(cms.galleries.htmlTabResultSceleton);
      resultTab.find('.cms-drop-down label').after($.fn.selectBox('generate',{
          values:[
              {value: 'title.desc',title: 'Title Ascending'}, 
              {value: 'title.desc',title: 'Title Descending'}, 
              {value: 'type.asc',title: 'Type Ascending'}, 
              {value: 'type.desc',title: 'Type Descending'}, 
              {value: 'datemodified.asc',title: 'Date Ascending'},
              {value: 'datemodified.desc',title: 'Date Descending'},
              {value: 'path.asc',title: 'Path Ascending'},
              {value: 'path.desc',title: 'Path Descending'}
          ],
          width: 150,
          /* TODO: bind sort functionality */
          select: function($this, self, value){$(self).closest('div.cms-list-options').attr('id')}}));
      // TODO blind out for the moment
      resultTab.find('.cms-drop-down').css('display','none');
      
      var typesTab = $(cms.galleries.htmlTabTypesSceleton);
      typesTab.find('.cms-drop-down label').after($.fn.selectBox('generate',{
          values:[
              {value: 'title,asc',title: 'Title Ascending'}, 
              {value: 'title,desc',title: 'Title Descending'}         
          ],
          width: 150,
          /* bind sort functionality to selectbox */
          select: function($this, self, value){
                  var criteria = $(self).closest('div.cms-list-options').attr('id');
                  criteria = criteria.replace('options', '');
                  var params = value.split(',');
                  var sortedArray = sortList(cms.galleries.searchCriteriaListsAsJSON[criteria], params[0], params[1]);
                  cms.galleries.refreshCriteriaList(sortedArray, criteria, params[0]);
              }
          })); 
          
      var galleriesTab = $(cms.galleries.htmlTabGalleriesSceleton);
      galleriesTab.find('.cms-drop-down label').after($.fn.selectBox('generate',{
          values:[
              {value: 'title,asc',title: 'Title Ascending'}, 
              {value: 'title,desc',title: 'Title Descending'},
              {value: 'gallerytypeid,asc',title: 'Type Ascending'}, 
              {value: 'gallerytypeid,desc',title: 'Type Descending'}          
          ],
          width: 150,
          /* bind sort functionality to selectbox */
          select: function($this, self, value){
                  var criteria = $(self).closest('div.cms-list-options').attr('id');
                  criteria = criteria.replace('options', '');
                  var params = value.split(',');
                  var sortedArray = sortList(cms.galleries.searchCriteriaListsAsJSON[criteria], params[0], params[1]);
                  cms.galleries.refreshCriteriaList(sortedArray, criteria, params[0]);
              }
          }));
      
      var categoriesTab = $(cms.galleries.htmlTabCategoriesSceleton);
      categoriesTab.find('.cms-drop-down label').after($.fn.selectBox('generate',{
          values:[
              {value: 'path,asc',title: 'Hierarchy'},
              {value: 'title,asc',title: 'Title Ascending'}, 
              {value: 'title,desc',title: 'Title Descending'}         
          ],
          width: 150,
          /* bind sort functionality to selectbox */
          select: function($this, self, value){
                  var criteria = $(self).closest('div.cms-list-options').attr('id');
                  criteria = criteria.replace('options', '');
                  var params = value.split(',');
                  var sortedArray = sortList(cms.galleries.searchCriteriaListsAsJSON[criteria], params[0], params[1]);
                  cms.galleries.refreshCriteriaList(sortedArray, criteria, params[0]);
              }
          }));  

      // add tabs html to tabs
      $('#' + cms.galleries.idTabs)
          .append(resultTab)
          .append(typesTab)
          .append(galleriesTab)
          .append(categoriesTab)
          .append(cms.galleries.htmlTabFTSeachSceleton);
      
      //TODO: blind out quick search dialog for the moment
      $('span.cms-ft-search').css('display', 'none');
      
      // bind the select tab event, fill the content of the result tab on selection
      $('#' + cms.galleries.idTabs).tabs({
         select: function(event, ui) {
            // if result tab is selected
            if (ui.index == 0) {
               cms.galleries.fillResultTab();
            }
         }
      });
      
      
      cms.galleries.loadSearchLists();
      $('#' + cms.galleries.idTabs).tabs("select", 1);
      $('#' + cms.galleries.idTabs).tabs("disable", 0);
      
      // bind all other events at the end          
      // bind click, dbclick events on items in criteria lists
      $('#types li.cms-list, #galleries li.cms-list, #categories li.cms-list')
          .live('dblclick', cms.galleries.dblclickListItem)
          .live('click', cms.galleries.clickListItem);
      // bind dbclick event to the items in the result list
      $('#results li.cms-list').live('dblclick', cms.galleries.dblclickToShowPreview);
          
      // bind hover event on items in criteriaand result lists
      $('li.cms-list')
          .live('mouseover', function() {
             $(this).addClass(cms.galleries.classListItemHover);
          }).live('mouseout', function() {
             $(this).removeClass(cms.galleries.classListItemHover);
      });        
      
      // add active class to checkbox of search tab  
      $('#searchInTitle, #searchInContent')
          .click(function() {
             $(this).toggleClass(cms.galleries.classListItemActive);
          });
      
      $('#searchQuery > input').blur(function() {
         cms.galleries.searchObject.query = $(this).val();
         cms.galleries.searchObject.isChanged.query = true;
      });           
         
      // bind click events to remove search criteria html from result tab            
      $('div.cms-search-remove').live('click', cms.galleries.removeCriteria);
      
      // bind the hover and click events to the ok button under the criteria lists    
      $('.cms-search-options button').hover(function() {
         $(this).addClass('ui-state-hover');
      }, function() {
         $(this).removeClass('ui-state-hover');
      }).click(function() {
         //switch to result tab index = 0
         $('#' + cms.galleries.idTabs).tabs("enable", 0);
         $('#' + cms.galleries.idTabs).tabs('select', 0);
      });
      
      //bind dbclick event to items in the result list to show the preview
      
   }
   
   
   
   /**
    * Add html for search criteria to result tab
    *
    * @param {String} content the nice-list of items for given search criteria
    * @param {String} searchCriteria the given search criteria
    */
   var addCreteriaToTab = cms.galleries.addCreteriaToTab =  function(/** String*/content, /** String*/ searchCriteria) {
      var target = $('<span id="selected' + searchCriteria + '" class="cms-criteria ui-widget-content ui-state-hover ui-corner-all"></span>')
          .appendTo($('.cms-result-criteria'));
      target.append('<div class="cms-search-title">' + content + '</div>')
          .append('<div class="cms-search-remove ui-icon ui-icon-closethick ui-corner-all"></div>');
   }

   var configContentTypes = [1, 2, 3, 4, 5, 6, 7, 146, 147, 149];
   
   /**
    * Loads the lists with available resource types, galleries ans categories via ajax call.
    * TODO: generalize to make it possible to load some preselected
    */
   var loadSearchLists = cms.galleries.loadSearchLists = function() {
      $.ajax({
         'url': vfsPathAjaxJsp,
         'data': {
            'action': 'all',
            'data': JSON.stringify({
               'types': configContentTypes
            })
         },
         'type': 'POST',
         'dataType': 'json',
         'success': cms.galleries.fillCriteriaTabs
      });
   }
   
   /**
    * Fills the list in the search criteria tabs.
    *
    * @param {Object} JSON map object
    */
   var fillCriteriaTabs = cms.galleries.fillCriteriaTabs = function(/**JSON*/data) {
      cms.galleries.searchCriteriaListsAsJSON = data;
      if (cms.galleries.searchCriteriaListsAsJSON.galleries) {
         cms.galleries.fillGalleries(cms.galleries.searchCriteriaListsAsJSON.galleries);
      }
      if (cms.galleries.searchCriteriaListsAsJSON.categories) {
         cms.galleries.fillCategories(cms.galleries.searchCriteriaListsAsJSON.categories, 'path');
      }
      if (cms.galleries.searchCriteriaListsAsJSON.types) {
         cms.galleries.fillTypes(cms.galleries.searchCriteriaListsAsJSON.types);
      }     
      // TODO: go through html and the search object and mark the already selected search criteria     
   }
   
   /**
    * Loads the lists with available resource types, galleries and categories via ajax call.
    * TODO: generalize to make it possible to load some preselected
    */
   var loadSearchResults = cms.galleries.loadSearchResults = function() {
      //alert('loadSearchResults');
      cms.galleries.searchObject.page = 1;
      $.ajax({
         'url': vfsPathAjaxJsp,
         'data': {
            'action': 'search',
            'data': JSON.stringify({
               'querydata': cms.galleries.searchObject
            })
         },
         'type': 'POST',
         'dataType': 'json',
         'success': cms.galleries.fillResultList
      });
   }
   
   var fillResultList = cms.galleries.fillResultList = function(/**JSON*/data) {
      // remove old list with search results and pagination
      $('#results > ul').empty();
      $('div.result-pagination').empty();
      var resultCriteriaHeight = $('.cms-result-criteria').height();
      var scrollingHeight = 290;
      if(resultCriteriaHeight > 30 && resultCriteriaHeight < 80) {
          scrollingHeight = 265;
      } else if (resultCriteriaHeight > 79 && resultCriteriaHeight < 110) {
          scrollingHeight = 235;
      } else if (resultCriteriaHeight > 111) {
          scrollingHeight = 210;
      }      
      $('#results').height(scrollingHeight);
                      
      if (data.searchresult.resultcount > 0) {
         // display
         cms.galleries.fillResultPage(data);
         // initialize pagination for result list, if there are many pages                  
         if (data.searchresult.resultcount > cms.galleries.searchObject.matchesperpage) {
            var firsttime = true;
            $('div.result-pagination').pagination(data.searchresult.resultcount, {
               items_per_page: cms.galleries.searchObject.matchesperpage,
               callback: function(page_id, jq) {
                  if (!firsttime) {
                     var currentPage = page_id + 1; 
                     if ($('#searchresults_page' + currentPage).children().length == 0) {
                        // adjust the page_id in the search object and load search results for this page
                        cms.galleries.searchObject.page = currentPage;
                        $.ajax({
                           'url': vfsPathAjaxJsp,
                           'data': {
                              'action': 'search',
                              'data': JSON.stringify({
                                 'querydata': cms.galleries.searchObject
                              })
                           },
                           'type': 'POST',
                           'dataType': 'json',
                           'success': cms.galleries.fillResultPage
                        });
                     }
                  } else {
                     firsttime = false;
                  }
               },
               prev_text: 'Prev',
               next_text: 'Next',
               prev_show_always: false,
               next_show_always: false,
               num_edge_entries: 1
            });
         }
      } else {
            // handle empty list for search
      }
   }
     
   var fillResultPage = cms.galleries.fillResultPage = function(pageData) {           
      var target = $('#results > ul').empty().removeAttr('id').attr('id', 'searchresults_page' + pageData.searchresult.resultpage);
      $.each(pageData.searchresult.resultlist, function() { 
          $('<li></li>').appendTo(target).attr('rel', this.path).addClass('cms-list').append(this.itemhtml);        
            // $(target).append(cms.galleries.listResultElement(this.title, this.path, this.icon));
      });
      
     /*
      * '<li class="cms-list"></li>').attr('rel', itemPath).append('<div class="cms-list-item ui-widget-content ui-state-default ui-corner-all">\
                             <div class="cms-list-image" style="background-image: url(' + itemIcon + ');"></div>\
                             <div class="cms-list-itemcontent">\
                                 <div class="cms-list-title">' + itemTitle + '</div>\
                                 <div class="cms-list-url">' +
      itemPath +
      '</div>\
                             </div>\
                         </div>'
      */ 
   }
   
   /**
    * Fills the list in the search criteria tabs.
    *
    * @param {Object} JSON map object
    */
   var refreshCriteriaList = cms.galleries.refreshCriteriaList = function(/**Array*/sortedList, /**String*/ criteria, option) {
   
      if (criteria == 'galleries') {
         cms.galleries.refreshGalleries(sortedList);
      } else if (criteria == 'categories') {
         cms.galleries.refreshCategories(sortedList, option);
      } else if (criteria == 'types') {
         cms.galleries.refreshTypes(sortedList);
      }
      // TODO: go through html and the search object and mark the already selected search criteria     
   }
   
   /**
    * Creates the HTML for the list of available categories from the given JSON map data.
    * @param {Object} JSON map object with categories
    * @param {Object} optional flag to set the categories view. Schould be 'path' for hierarchal view.
    */
   var fillCategories = cms.galleries.fillCategories = function(/**Json*/categories, /**String*/ option) {
   
      // switch on or to switch off the hierarchic view
      var classActive = '';
      if (option == 'path') {
         classActive = 'cms-active-level';
      }
      //add the types to the list
      for (var i = 0; i < categories.length; i++) {
         //$('#categories > ul').append(listCategoryElement(categories[i].title, categories[i].path, categories[i].level, classActive));
         $('<li></li>').appendTo('#categories > ul')
             .attr('rel', categories[i].path).addClass('cms-list ' + classActive + ' ' + classConstLevel + categories[i].level).addClass('cms-list-with-checkbox')
             .append('<div class="cms-list-checkbox"></div>')             
             .append(categories[i].itemhtml);
      }
      // set isChanged flag, so the search will be send to server
      cms.galleries.searchObject.isChanged.categories = true;
   }
   
   /**
    * Refreshes the order of the item in the list as given in parameter array.
    *
    * @param {JSON} categories the ordered list with categories
    * @param {String} optional flag to set the categories view. Schould be 'path' for hierarchal view.
    */
   var refreshCategories = cms.galleries.refreshCategories = function(/**JSON*/categories, /**String*/ option) {
   
      // set the flag to switch on or to switch off the hierarchic view
      var isActive = false;
      if (option == 'path') {
         isActive = true;
      }
      
      // reorder the item in the list      
      $.each(categories, function() {
         $("li[rel='" + this.path + "']").appendTo('#categories > ul').toggleClass('cms-active-level', isActive);
      });
   }
   
   /** 
    * Creates the HTML for the list of available galleries from the given JSON array data.
    *
    * Comment: this is an adjusted copy from galleryfunctions.js of the old galleries
    *
    * @param {Object} JSON object with categories
    */
   var fillGalleries = cms.galleries.fillGalleries = function(/**JSON*/galleries) {
      // add the galleries to the list
      for (key in galleries) {
         //$('#galleries > ul').append(listGalleryElement(galleries[key].title, galleries[key].path, galleries[key].icon));
         $('<li></li>').appendTo('#galleries > ul')
             .attr('rel', galleries[key].path).addClass('cms-list').addClass('cms-list-with-checkbox')
             .append('<div class="cms-list-checkbox"></div>')
             .append(galleries[key].itemhtml);
         $('li[rel=' + galleries[key].path + ']').data('type', galleries[key].type);
      }
      // set isChanged flag, so the search will be send to server
      cms.galleries.searchObject.isChanged.galleries = true;
   }
   
   /**
    * Refreshes the order of galleries list as given in parameter array.
    *
    * @param {JSON} galleries the ordered list with galleries
    */
   var refreshGalleries = cms.galleries.refreshGalleries = function(/**JSON*/galleries) {
   
      // reorder the item in the list      
      $.each(galleries, function() {
         $("li[rel='" + this.path + "']").appendTo('#galleries > ul');
      });
   }
   
   /** 
    * Creates the HTML for the list of available types from the given JSON array data.
    *
    * @param {Object} JSON object with resource types
    */
   var fillTypes = cms.galleries.fillTypes = function(/**JSON*/types) {
      // add the types to the list
      for (var i = 0; i < types.length; i++) {
         var currType = types[i];
         //$('#types > ul').append(listTypeElement(currType.title, currType.typeid, currType.info, currType.icon));
         $('<li></li>').appendTo('#types > ul')
             .attr('rel', currType.typeid).addClass('cms-list').addClass('cms-list-with-checkbox')
             .append('<div class="cms-list-checkbox"></div>')
             .append(currType.itemhtml);
         $('li[rel=' + currType.typeid + ']').data('galleryTypes', currType.gallerytypeid);
      }
      // set isChanged flag, so the search will be send to server
      cms.galleries.searchObject.isChanged.types = true;
   }
   
   /**
    * Refreshes the order of types list as given in parameter array.
    *
    * @param {JSON} types the ordered list with types
    */
   var refreshTypes = cms.galleries.refreshTypes = function(/**JSON*/types) {
      // reorder the item in the list      
      $.each(types, function() {
         $("li[rel='" + this.typeid + "']").appendTo('#types > ul');
      });
   }
   
   /**
    * Adds the search criteria html to the result tab.
    */
   var fillResultTab = cms.galleries.fillResultTab = function() {
      var searchEnables = false;
      // display the search criteria
      $.each(cms.galleries.keysSearchObject, function() {
         var searchCriteria = this;
         
         if (cms.galleries.searchObject.isChanged[searchCriteria]) {
            // is true, if at least one criteria isChanged
            searchEnables = true;
            
            var singleSelect = cms.galleries.criteriaStr[searchCriteria][0];
            var multipleSelect = cms.galleries.criteriaStr[searchCriteria][1];
            var titles = '';
            // remove criteria button from result tab
            $('#selected' + searchCriteria).remove();
            
            if (searchCriteria == 'query') {
               var searchQuery = $('#searchQuery input').val();
               titles = singleSelect.concat(searchQuery);
               cms.galleries.addCreteriaToTab(titles, searchCriteria);
               cms.galleries.searchObject.isChanged[searchCriteria] = false;
            } else {
               var selectedLis = $('#' + searchCriteria).find('.' + cms.galleries.classListItemActive).find('.' + cms.galleries.classListItemTitle);
               // if any search criteria is selected
               if (selectedLis.length == 1) {
                  titles = singleSelect.concat($(selectedLis[0]).text());
                  cms.galleries.addCreteriaToTab(titles, searchCriteria);
               } else if (selectedLis.length > 1) {
                  $.each(selectedLis, function() {
                     if (titles.length == 0) {
                        titles = multipleSelect.concat($(this).text());
                     } else {
                        titles = titles.concat(", ").concat($(this).text());
                     }
                  });
                  cms.galleries.addCreteriaToTab(titles, searchCriteria);
               }
               cms.galleries.searchObject.isChanged[searchCriteria] = false;
            }
         }
      });
         
      // display the search results
      if (searchEnables) {
         cms.galleries.loadSearchResults();
      }
      
      
   }
   
   /**
    * Callback function for the one click event in the gallery list
    */
   var clickListItem = cms.galleries.clickListItem = function() {         
          // id of the li tag and type of search 
          var itemId = $(this).attr('rel');
          var itemCriteria = $(this).closest('div').attr('id');

          // adjust the active status of the gallery in the gallery list        
          var index = $.inArray(itemId, cms.galleries.searchObject[itemCriteria]);
          // case 1: gallery is selected, -> deselect the gallery on second click
          if (index != -1) {
             // remove gallery path from search object
             cms.galleries.searchObject[itemCriteria].splice(index, 1);
             // remove highlighting
             $(this).removeClass(cms.galleries.classListItemActive);
             
             // set isChanged flag, so the search will be send to server
             cms.galleries.searchObject.isChanged[itemCriteria] = true;
                          
             // case 2: gallery is not selected yet, -> select the gallery on click
          } else {
             // push the gallery path to the search object
             cms.galleries.searchObject[itemCriteria].push(itemId);
             // add highlighting
             $(this).addClass(cms.galleries.classListItemActive);
             
             $('#' + cms.galleries.idTabs).tabs("enable", 0);
             
             // set isChanged flag, so the search will be send to server
             cms.galleries.searchObject.isChanged[itemCriteria] = true;             
          }     
   }
   
   /**
    * Callback function for the double click event in the search criteria list
    */
   var dblclickListItem = cms.galleries.dblclickListItem = function() {   
     
      // id of the li tag and type of search 
      var itemId = $(this).attr('rel');
      var itemCriteria = $(this).closest('div').attr('id');
      
      // adjust the active status of the gallery in the gallery list        
      var index = $.inArray(itemId, cms.galleries.searchObject[itemCriteria]);
      // case 1: gallery is not selected, -> select the gallery
      if (index == -1){
         // push the gallery path to the search object
         cms.galleries.searchObject[itemCriteria].push(itemId);
         // add highlighting
         $(this).addClass(cms.galleries.classListItemActive);         
         
         // set isChanged flag, so the search will be send to server
         cms.galleries.searchObject.isChanged[itemCriteria] = true;
         
      }      
      //switch to result tab index = 0
      $('#' + cms.galleries.idTabs).tabs("enable", 0);
      $('#' + cms.galleries.idTabs).tabs('select', 0);   
   }
   
   /**
    * Removes all selected items from the search object and adjust highlighting
    *
    * @param {String} searchCriteria key
    */
   var removeItemsFromSearchObject = cms.galleries.removeItemsFromSearchObject = function(/**String*/searchCriteria) {
      // if at least one item is selected
      if (cms.galleries.searchObject[searchCriteria].length > 0) {
         if (searchCriteria == 'query') {
            $('#searchQuery input').val('');
            // remove all items from search object                                                     
            cms.galleries.searchObject[searchCriteria] = '';
         } else {
            //remove highlighting for all selected items in the list
            $('#' + searchCriteria + ' li.cms-list').removeClass(cms.galleries.classListItemActive);
            // remove all items from search object                                                     
            cms.galleries.searchObject[searchCriteria] = [];
         }
         // refresh the searchResults
         cms.galleries.loadSearchResults();
      }
   }
   
   /**
    * Removes the html of the search criteria from the result tab and from search object
    */
   var removeCriteria = cms.galleries.removeCriteria = function() {
      var parentId = $(this).parent().attr('id');
      $.each(cms.galleries.keysSearchObject, function() {
         var selectedId = 'selected' + this;
         if (parentId == selectedId) {
            $('#' + selectedId).remove();
            cms.galleries.removeItemsFromSearchObject(this);
         }
      });
      
      // TODO: refresh the list of search results on the result tab
   }
   
   /**
    * Sorts the array of objects by given key and order.
    *
    * @param {Array} list the array to be sorted
    * @param {String} sortBy the name of the key
    * @param {String} sortOrder the sort order. It can be 'asc'(default) or 'desc'
    */
   var sortList = function(/** Array */list, /**String*/ sortBy, /**String*/ sortOrder) {
      var sortedArray = list;
      
      if (sortOrder == 'asc' || sortOrder == null) {
         sortedArray.sort(function(a, b) {
            if (a[sortBy] < b[sortBy]) {
               return -1;
            } else if (a[sortBy] > b[sortBy]) {
               return 1;
            } else {
               /*if (a['title'] < b['title']){
                return -1 ;
                } else if (a['title'] > b['title']) {
                return 1;
                } else {
                return 0;
                } */
               return 0;
            }
         });
      } else {
         sortedArray.sort(function(a, b) {
            if (a[sortBy] > b[sortBy]) {
               return -1;
            } else if (a[sortBy] < b[sortBy]) {
               return 1;
            } else {
               /*if (a['title'] > b['title']){
                return -1 ;
                } else if (a['title'] < b['title']) {
                return 1;
                } else {
                return 0;
                } */
               return 0;
            }
         });
         
      }
      return sortedArray;
   }
   
   
  var dblclickToShowPreview = cms.galleries.dblclickToShowPreview = function() {
      // id of the li tag and type of search 
      var itemId = $(this).attr('rel');
      var currPreviewId = $('#cms-preview').attr('rel');
      if (currPreviewId == null) {
          $('#cms-preview').attr('rel',itemId);
          loadItemPreview(itemId); 
      } else if (currPreviewId != null || itemId != currPreviewId) {
          $('#cms-preview').attr('rel', itemId);
          $('#cms-preview div.preview-area, #cms-preview div.edit-area').empty();
          loadItemPreview(itemId);
      } else {
          $('#cms-preview').fadeIn('slow');
      }
      

  } 
   
   /**
    * Loads the lists with available resource types, galleries ans categories via ajax call.
    * TODO: generalize to make it possible to load some preselected
    */
  var loadItemPreview = cms.galleries.loadItemPreview = function(/**String*/ itemId) {
      $.ajax({
         'url': vfsPathAjaxJsp,
         'data': {
            'action': 'preview',
            'data': JSON.stringify({
               'path': itemId
            })
         },
         'type': 'POST',
         'dataType': 'json',
         'success': cms.galleries.showItemPreview
      });
     
     //cms.galleries.showItemPreview(itemData);
     
   }     
   
   var showItemPreview = cms.galleries.showItemPreview = function(itemData) {
    
       showPreview(itemData['previewdata']['itemhtml']);
       showEditArea(itemData['previewdata']['properties']);
       
       
       $('#cms-preview').fadeIn('slow');
       
   }
   
   var showPreview = cms.galleries.showPreview = function(itemPreview) {
       $('.preview-area').append(itemPreview);
   }
   
   var showEditArea = cms.galleries.showEditArea = function(itemProperties) {    
       var target = $('.edit-area').append('<span id="previewSave" class="cms-preview-button ui-state-default ui-corner-all">Save</span>\
                                           <span id="previewPublish" class="cms-preview-button ui-state-default ui-corner-all">Publish</span>');
       
       $.each(itemProperties, function() {

              $('<div style="margin: 2px;"></div>').attr('rel', this.name).appendTo(target)
                   .append('<span class="cms-item-title" style="margin-right:10px; width: 100px;">' + this.name + '</span>')
                   .append('<span class="cms-item-edit" style=" width: 100px;">' + this.value + '</span>');
                   
               
           });       
       
       $('.cms-item-edit').directInput({
                     marginHack: true,
                     live: false,
                     setValue: markChangedProperty
               });
       $('#previewSave').click(saveChangedProperty);
       $('#publishSave').click(publishChangedProperty);
        
   }
   
   
   var markChangedProperty = cms.galleries.markChangedProperty = function(elem, input) {
      
      var previous = elem.text();
      var current = input.val();
      if (previous != current) {               
            elem.text(current);
            elem.addClass('cms-item-changed');
         
      }
      elem.css('display', '');
      input.remove();          
   }
   
   var saveChangedProperty = cms.galleries.saveChangedProperty = function() {
       var changedProperties = $('.cms-item-edit.cms-item-changed');
       
       // build json object with changed properties
       var changes = {
           'properties': []};
       $.each(changedProperties, function () {           
           var property = {};
           property['name'] =  $(this).closest('div').attr('rel');
           property['value'] = $(this).text();
           changes['properties'].push(property);
       });
       
       // save changes via ajax 
       if (changes['properties'].length != 0) {
          $.ajax({
             'url': vfsPathAjaxJsp,
             'data': {
                'action': 'subproperties',
                'data': JSON.stringify({
                   'path': $('#cms-preview').attr('rel'),
                   'properties': changes['properties']
                })
             },
             'type': 'POST',
             'dataType': 'json',
             'success': cms.galleries.refreshItemPreview
          });
       }
       
   }
   
   var refreshItemPreview = cms.galleries.refreshItemPreview = function (data) {
       
   }
   
   var publishChangedProperty = cms.galleries.publishChangedProperty = function() {
       alert('Publish');
   } 
   
   /*var defaultContentTypeHandler={
    'init': function(){},
    
    };*/
    
   /* var contentTypeHandlers={'default': defaultContentTypeHandler};

    var addContentTypeHandler=function(typeId, handler){
        contentTypeHandlers[typeId]= $.extend({}, defaultContentTypeHandler, handler);
    }


    var getContentHandler = function(typId){
        if (contentTypeHandlers[typId]){
            return contentHandler[typId];
        }
        return contentHandler['default'];
    }*/

/*

var specialHandler={
    'init': function(){}
}
addContentHandler(4, specialHandler);

*/
   
})(cms);