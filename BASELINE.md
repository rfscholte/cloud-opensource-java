Instructions to create a baseline:

* Execute `mvnw install` on this project

* Add the following to the targeted project:

        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-enforcer-plugin</artifactId>
          <version>3.5.0</version>
          <dependencies>
            <dependency>
              <groupId>com.google.cloud.tools</groupId>
              <artifactId>linkage-checker-enforcer-rules</artifactId>
              <version>1.5.14-SNAPSHOT</version>
            </dependency>
          </dependencies>
          <executions>
            <execution>
              <id>linkage-checker</id>
              <phase>verify</phase>
              <goals>
                <goal>enforce</goal>
              </goals>
              <configuration>
                <rules>
                  <linkageCheckerRule>
                    <baselineFile>baseline.xml</baselineFile> <!-- remove this line once created! -->
                    <exclusionFile>linkagechecker-exclusions.xml</exclusionFile>
                    <reportOnlyReachable>true</reportOnlyReachable>
                  </linkageCheckerRule>
                </rules>
                <fail>${linkage-checker.fail}</fail>
              </configuration>
            </execution>
          </executions>
        </plugin>
* Execute `mvn verify -DskipTests -Dlinkage-checker.fail=false`
* Open `baseline.xml` and put the content between `<LinkageCheckerFilters>` and `</LinkageCheckerFilters>` tags.
* Copy `LinkageCheckerFiltersReducer.java` to targeted project
* Execute `java LinkageCheckerFiltersReducer.java baseline.xml linkagechecker-exclusions.xml` (requires at least Java 17)