package serverSide;

import business.ICollectionEmployees;
import business.models.Employee;
import org.apache.commons.lang3.StringUtils;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * this class is thread safe,
 */
public class CollectionEmployeesImpl implements ICollectionEmployees {
    private static final Logger LOGGER = Logger.getLogger(CollectionEmployeesImpl.class.getSimpleName());
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private FileLoader fileLoader;
    protected Set<Employee> employees;
    protected LocalDate today= LocalDate.now(ZoneId.systemDefault());

    protected CollectionEmployeesImpl() {
    }

    public CollectionEmployeesImpl(String flatFilePath) throws IllegalArgumentException {
        try {
            fileLoader = StringUtils.isNotBlank(flatFilePath) ?
                    new FileLoader(flatFilePath) :
                    new FileLoader("serverSideFlatFiles/users.csv");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized Set<Employee> allEmployees() {
        if (employees == null) {
            employees = new TreeSet<>();
            loadUsers();
        }

        return employees;
    }

    @Override
    public Set<Employee> findEmployeesDateBirthdayToday() {
        return allEmployees().stream().filter(user -> {
            if (user.getDateOfBirth().isLeapYear() && !today.isLeapYear()) {
                return user.getDateOfBirth().getDayOfYear() == today.getDayOfYear() +1;
            }

            return user.getDateOfBirth().getMonth().getValue() == today.getMonth().getValue()
                    && user.getDateOfBirth().getDayOfMonth() == today.getDayOfMonth();
        }).collect(Collectors.toSet());
    }

    /**
     * iterate over flat file records, instantiate {@link Employee} entities and fill {@link Set<Employee>} employees.
     */
    private void loadUsers() {
        fileLoader.generateBufferedReader().lines().skip(1).forEach(line -> {
            String[] lineToUserPropValues = line.split(",");
            List<String> formatLine = Stream.of(lineToUserPropValues)
                    .filter(str -> StringUtils.isNotBlank(str))
                    .map(str -> str.trim())
                    .collect(Collectors.toList());

            if (formatLine.size() == Employee.class.getDeclaredFields().length) {
                LocalDate dateOfBirth = null;
                try {
                    dateOfBirth = LocalDate.parse(formatLine.get(2), dateTimeFormatter);
                    Employee employee = new Employee(formatLine.get(0),
                            formatLine.get(1),
                            dateOfBirth,
                            formatLine.get(3));
                    employees.add(employee);
                } catch (DateTimeParseException ex) {
                    LOGGER.severe("can't parse given line segment: "+formatLine.get(2));
                    ex.printStackTrace();
                }
            }

        });

    }

}
