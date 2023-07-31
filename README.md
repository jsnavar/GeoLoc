## GeoLoc

Map coordinates to labels using a quadtree of Spark datasets.

### Indexing
Data is read into a dataframe from a relational table with schema:
```sql
  CREATE TABLE(label TEXT, polygon JSONB);
```

That dataframe is then augmented with bounding boxes, generating a `Dataset[LabeledBox]`:
```scala
  case class LabeledBox(label: String, bbox: BBox, polygon: Seq[Point])
```
and finally, the dataset is splitted into a quadtree, such that each leaf contains the 
`LabeledBox` objects with bounding boxes intersecting its area.

### Searching
Given a point, searching takes three steps:
- First, it uses the quadtree to find a leaf containing the point. By construction, that leaf has all the polygons that could cover the point.
- Then, it filters the leaf's dataset to get the polygons that actually have the point in the interior, using a ray crossings algorithm. For performance reasons, it checks the bounding boxes before, and only computes the ray crossings if the point is in the bbox.
- Finally, it collects the labels from the resulting objects.

### Data
The system was tested with data from [GADM 4.1](https://gadm.org/data.html), which can not be redistributed without permission. 

GeoJSON files from gadm can be downloaded into `data/gadm/`, and then loaded into postgres using the script `data/load_countries.sh`. It is also necessary to provide username and password for postgres in `src/main/scala/secret.scala`.

Some metadata (queries in readme/data.sql):

Property | Value
-------- | --------
Number of countries / territories | 251
Number of level 1 subdivisions | 3643
Total number of polygons | 120939
Average polygon size   | 37.5533
Stddev of polygon size | 519.276
Maximum polygon size   | 106669
Minimum polygon size   | 4

Largest polygons:
               label               |  len   
-----------------------------------|--------
 Antarctica;                       | 106669
 Canada;Nunavut                    |  67813
 UnitedStates;Alaska               |  49236
 Canada;Nunavut                    |  47650
 Canada;Nunavut                    |  38685
 Canada;Québec                     |  29713
 Russia;Sakha                      |  24159
 Russia;Krasnoyarsk                |  22138
 Australia;WesternAustralia        |  19021
 Canada;NewfoundlandandLabrador    |  17213
 Canada;NewfoundlandandLabrador    |  17104
 Russia;Yamal-Nenets               |  16139
 Chile;AyséndelGeneralIbañezdelCam |  14217

Top 10 countries by number of polygons
    country    |  np   
---------------|-------
 Canada        | 24510
 Chile         | 10120
 UnitedStates  |  8516
 Indonesia     |  7539
 Australia     |  6646
 Russia        |  5802
 Sweden        |  3359
 Norway        |  3137
 Finland       |  2884
 UnitedKingdom |  2495


Top 3 subdivisions of top 5 countries by number of polygons:
   country    |            l1div            | npoly 
--------------|-----------------------------|-------
 Australia    | WesternAustralia            |  2734
 Australia    | Tasmania                    |  1866
 Australia    | Queensland                  |  1185
 Canada       | Nunavut                     | 11906
 Canada       | Québec                      |  4107
 Canada       | NewfoundlandandLabrador     |  2868
 Chile        | MagallanesyAntárticaChilena |  5773
 Chile        | AyséndelGeneralIbañezdelCam |  3567
 Chile        | Valparaíso                  |   356
 Indonesia    | KepulauanRiau               |  1524
 Indonesia    | MalukuUtara                 |   646
 Indonesia    | SulawesiTengah              |   488
 UnitedStates | Alaska                      |  4152
 UnitedStates | Florida                     |  1145
 UnitedStates | Louisiana                   |   845

### Interface
GeoLoc includes two interfaces: an interactive stdin/stdout interface, and an HTTP one. An example of the interactive interface is:
```
 [info] Running
-73.889,-38.377
[info] Point(-73.889,-38.377) is in Chile;Bío-Bío
-113.364, 55.449
[info] Point(-113.364,55.449) is in Canada;Alberta
85.62
85.62, 65.839
[info] Point(85.62,65.839) is in Russia;Krasnoyarsk
142.1861,27.0371
[info] Point(142.1861,27.0371) was not found

```
As seen in the example, invalid inputs are simply ignored.
On the other hand, the http interface listens on port 8080 for GET requests on `/locate/<longitude>/<latitude>`. For example:
```
 $ curl localhost:8080/locate/130.66/30.997
["Japan;Kagoshima"]
```
This interface returns a 400 code if the coordinates are invalid, and a 404 if it does not find the point.

