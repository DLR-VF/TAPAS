package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.trafficgeneration.SchemeProviderConfiguration;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.model.choice.DiscreteChoiceModel;
import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;
import de.dlr.ivf.tapas.model.choice.DiscreteDistributionFactory;
import de.dlr.ivf.tapas.model.choice.DiscreteProbability;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.plan.StayHierarchies;
import de.dlr.ivf.tapas.model.plan.StayHierarchy;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.TPS_CarCode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.simulation.trafficgeneration.SchemeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.*;

@Lazy
@Configuration
public class SchemeBeanFactory {

    private final TPS_DB_IO dbIo;

    @Autowired
    public SchemeBeanFactory(TPS_DB_IO dbIo){
        this.dbIo = dbIo;
    }

    @Bean
    public SchemeProvider schemeProvider(SchemeProviderConfiguration configuration,
                                         Map<Integer, List<DiscreteProbability<SchemeClass>>> schemeClassProbabilitiesByPersonGroup,
                                         PersonGroups personGroups){

        DiscreteDistributionFactory<SchemeClass> distributionFactory = new DiscreteDistributionFactory<>();

        Map<TPS_PersonGroup, DiscreteDistribution<SchemeClass>> schemeClassDistributions = new HashMap<>();

        for(TPS_PersonGroup personGroup : personGroups.getPersonGroups()){

            List<DiscreteProbability<SchemeClass>> probabilities = schemeClassProbabilitiesByPersonGroup.get(personGroup.getCode());

            if(probabilities == null){
                throw new IllegalArgumentException("No scheme class probabilities for person group:" + personGroup);
            }

            DiscreteDistribution<SchemeClass> distribution = distributionFactory.newNormalizedDiscreteDistribution(probabilities);
            schemeClassDistributions.put(personGroup, distribution);
        }

        DiscreteChoiceModel<SchemeClass> schemeClassChoiceModel = new DiscreteChoiceModel<>(configuration.schemeClassChoiceSeed());
        DiscreteChoiceModel<Scheme> schemeChoiceModel = new DiscreteChoiceModel<>(configuration.schemeChoiceSeed());

        return new SchemeProvider(schemeClassDistributions, schemeClassChoiceModel, schemeChoiceModel);
    }

    @Bean
    public Map<Integer, List<DiscreteProbability<SchemeClass>>> schemeClassProbabilitiesByPersonGroup(
            Collection<SchemeClassDistributionDto> schemeClassDistributionDtos,
            Map<Integer, SchemeClass> schemeClassesByClassId){

        return schemeClassDistributionDtos
                .stream()
                .collect(groupingBy(SchemeClassDistributionDto::getPersonGroup,
                                mapping(dto -> new DiscreteProbability<>(schemeClassesByClassId.get(dto.getSchemeClassId()), dto.getProbability()), toList())));
    }

    @Bean
    public Map<Integer, SchemeClass> schemeClasses(SchemeProviderConfiguration configuration,
                                                   Collection<SchemeClassDto> schemeClassDtos,
                                                   Map<Integer, List<Scheme>> schemesByClassId) {

        Map<Integer, SchemeClass> schemeClasses = new HashMap<>(schemeClassDtos.size());

        for(SchemeClassDto schemeClassDto :schemeClassDtos){
            double mean = schemeClassDto.getAvgTravelTime() * configuration.timeSlotLength();
            double deviation = mean * schemeClassDto.getProcStdDev();

            List<Scheme> schemesInClass = schemesByClassId.get(schemeClassDto.getId());
            DiscreteDistributionFactory<Scheme> distributionFactory = new DiscreteDistributionFactory<>();
            DiscreteDistribution<Scheme> schemeDistribution = distributionFactory.newUniformNormalizedDiscreteDistribution(schemesInClass);

            int schemeClassId = schemeClassDto.getId();
            SchemeClass schemeClass = new SchemeClass(schemeClassId, mean, deviation, schemeDistribution);
            schemeClasses.put(schemeClassId, schemeClass);
        }

        return schemeClasses;
    }

    @Bean
    public StayHierarchies stayHierarchies(Map<Integer, Set<Tour>> toursBySchemeId,
                                           @Qualifier("stayPriorityDurationStartTimeComparator") Comparator<Stay> stayComparator){

        StayHierarchies stayHierarchies = new StayHierarchies();

        toursBySchemeId.values()
                .stream()
                .flatMap(Collection::stream)
                .forEach(tour -> stayHierarchies.addStayHierarchy(tour.id(), newStayHierarchy(tour.stays(), stayComparator)));

        return stayHierarchies;
    }

    @Bean("stayPriorityDurationStartTimeComparator")
    public Comparator<Stay> stayPriorityDurationStartTimeComparator(){
        return Comparator.comparing(Stay::priority).thenComparing(Stay::duration).reversed().thenComparing(Stay::startTime);
    }

    public StayHierarchy newStayHierarchy(Set<Stay> stays, Comparator<Stay> stayComparator){

        List<Stay> sortedStaysByStartTime = stays.stream().sorted(Comparator.comparingInt(Stay::startTime)).toList();
        Stay beginningStay = sortedStaysByStartTime.getFirst();
        Stay endingStay = sortedStaysByStartTime.getLast();

        SortedSet<Stay> processedStays = new TreeSet<>(stayComparator);
        List<Stay> sortedStaysByComparator = stays.stream().sorted(stayComparator).toList();

        StayHierarchy stayHierarchy = new StayHierarchy(sortedStaysByComparator);

        for (Stay stay : sortedStaysByComparator) { // first entry-> highest Prio
            Stay prevStayInHierarchy = beginningStay;
            Stay nextStayInHierarchy = endingStay;

            for (Stay processedStay : processedStays) {

                //higher Prio, older than prev and younger than stay?
                if (processedStay.startTime() >= prevStayInHierarchy.startTime() &&
                        processedStay.startTime() < stay.startTime() && processedStay != nextStayInHierarchy) {
                    prevStayInHierarchy = processedStay;
                }

                //higher Prio, younger than next and older than stay?
                if (processedStay.startTime() <= nextStayInHierarchy.startTime() &&
                        processedStay.startTime() > stay.startTime() && processedStay != prevStayInHierarchy) {
                    nextStayInHierarchy = processedStay;
                }

            }

            stayHierarchy.addPrecedingStay(stay, prevStayInHierarchy);
            stayHierarchy.addSucceedingStay(stay, nextStayInHierarchy);
            processedStays.add(stay);
        }

        return stayHierarchy;
    }

    @Bean
    public Map<Integer, List<Scheme>> schemesByClassID(Collection<SchemeDto> schemeDtos,
                                                       Map<Integer, Set<Tour>> toursBySchemeId){

        return schemeDtos.stream()
                .map(dto -> new Scheme(dto.getId(), dto.getSchemeClassId(), toursBySchemeId.get(dto.getId())))
                .collect(groupingBy(Scheme::schemeClassId, toList()));
    }

    @Bean
    public Map<Integer, Set<Tour>> toursBySchemeId(SchemeProviderConfiguration configuration,
                                                   Collection<EpisodeDto> episodeDtos,
                                                   Activities activities){

        Map<Integer, Collection<EpisodeDto>> episodesBySchemeId = episodeDtos
                .stream()
                .collect(groupingBy(EpisodeDto::getSchemeId, toCollection(ArrayList::new)));

        Map<Integer, Set<Tour>> toursBySchemeId = new HashMap<>();

        AtomicInteger tourIdProvider = new AtomicInteger(0);

        for (Map.Entry<Integer, Collection<EpisodeDto>> entry : episodesBySchemeId.entrySet()) {

            int schemeId = entry.getKey();
            Collection<EpisodeDto> episodesInScheme = entry.getValue();
            Set<Tour> tours = generateToursFromEpisodes(configuration.timeSlotLength(), episodesInScheme,
                    activities, configuration.tripActivityCode(), tourIdProvider);

            toursBySchemeId.put(schemeId, tours);
        }

        return toursBySchemeId;
    }

    public Set<Tour> generateToursFromEpisodes(int timeSlotLength,
                                               Collection<EpisodeDto> episodes,
                                               Activities activities,
                                               int tripActivityCode,
                                               AtomicInteger tourIdProvider) {

        List<EpisodeDto> sortedEpisodesInScheme = episodes
                .stream()
                .sorted(Comparator.comparingInt(EpisodeDto::getStart))
                .toList();

        Map<Integer, NavigableSet<Trip>> tripsByTourNumber = new HashMap<>();
        Map<Integer, NavigableSet<Stay>> staysByTourNumber = new HashMap<>();

        for(int i = 1; i < sortedEpisodesInScheme.size() - 1; i = i+2){

            EpisodeDto startEpisode = sortedEpisodesInScheme.get(i-1);
            EpisodeDto tripEpisode = sortedEpisodesInScheme.get(i);
            EpisodeDto endEpisode = sortedEpisodesInScheme.get(i+1);

            validateEpisodesOrThrow(startEpisode, endEpisode, tripEpisode, tripActivityCode);

            Activity startActivity = activities.getActivityByZbeCode(startEpisode.getActCodeZbe());
            Activity endActivity = activities.getActivityByZbeCode(endEpisode.getActCodeZbe());

            Stay startStay = new Stay(startEpisode.getStart(), startEpisode.getDuration(), startEpisode.getActCodeZbe(),startActivity.priority());
            Stay endStay  = new Stay(endEpisode.getStart(), endEpisode.getDuration(), endEpisode.getActCodeZbe(), endActivity.priority());

            int tripPriority = activities.getActivity(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, endStay.activity())
                    .getCode(TPS_ActivityConstant.TPS_ActivityCodeType.PRIORITY);

            Trip trip = new Trip(startStay, endStay, tripEpisode.getStart(), tripEpisode.getDuration() * timeSlotLength, tripPriority);

            int tourNumber = tripEpisode.getTourNumber();

            tripsByTourNumber.computeIfAbsent(tourNumber, tourNum -> new TreeSet<>(Comparator.comparingInt(Trip::startTime))).add(trip);
            staysByTourNumber.computeIfAbsent(tourNumber, tourNum -> new TreeSet<>(Comparator.comparingInt(Stay::startTime))).add(startStay);
            staysByTourNumber.computeIfAbsent(tourNumber, tourNum -> new TreeSet<>(Comparator.comparingInt(Stay::startTime))).add(endStay);
        }

        return tripsByTourNumber
                .keySet()
                .stream()
                .map(tourId -> new Tour(tourIdProvider.incrementAndGet(), tourId, tripsByTourNumber.get(tourId), staysByTourNumber.get(tourId)))
                .collect(toUnmodifiableSet());
    }

    private void validateEpisodesOrThrow(EpisodeDto start, EpisodeDto end, EpisodeDto trip, int tripActivityCode){
        if(start.getActCodeZbe() == tripActivityCode){
            throw new IllegalArgumentException("Start episode is a trip but it should be a stay.");
        }
        if(end.getActCodeZbe() == tripActivityCode){
            throw new IllegalArgumentException("End episode is a trip but it should be a stay.");
        }
        if(trip.getActCodeZbe() != tripActivityCode){
            throw new IllegalArgumentException("Trip episode is not of activity code " + trip.getActCodeZbe()+". Set tripActivityCode to "+trip.getActCodeZbe());
        }
    }

    @Bean
    public Activities activities(Collection<TPS_ActivityConstant> activityConstants, Collection<Activity> activities) {

        return new Activities(activityConstants, activities);
    }

    @Bean
    public Collection<TPS_ActivityConstant> activityConstants(Collection<ActivityDto> activityDtos){

        Collection<TPS_ActivityConstant> activities = new ArrayDeque<>(activityDtos.size());

        for(ActivityDto activityDto : activityDtos){

            String attribute = activityDto.getAttribute();

            var mctConstant = new TPS_InternalConstant<>(activityDto.getNameMct(), activityDto.getCodeMct(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeMct()));
            var tapasConstant = new TPS_InternalConstant<>(activityDto.getNameTapas(), activityDto.getCodeTapas(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeTapas()));
            var priorityConstant = new TPS_InternalConstant<>(activityDto.getNamePriority(), activityDto.getCodePriority(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypePriority()));
            var zbeConstant = new TPS_InternalConstant<>(activityDto.getNameZbe(), activityDto.getCodeZbe(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeZbe()));
            var votConstant = new TPS_InternalConstant<>(activityDto.getNameVot(), activityDto.getCodeVot(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeVot()));

            TPS_ActivityConstant act = TPS_ActivityConstant.builder()
                    .id(activityDto.getId())
                    .internalConstant(mctConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.MCT,mctConstant)
                    .internalConstant(tapasConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS,tapasConstant)
                    .internalConstant(priorityConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.PRIORITY, priorityConstant)
                    .internalConstant(zbeConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, zbeConstant)
                    .internalConstant(votConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.VOT, votConstant)
                    .attribute(!(attribute == null || attribute.equals("null"))
                            ? TPS_ActivityConstant.TPS_ActivityConstantAttribute.valueOf(activityDto.getAttribute())
                            : TPS_ActivityConstant.TPS_ActivityConstantAttribute.DEFAULT)
                    .isFix(activityDto.isFix())
                    .isTrip(activityDto.isTrip())
                    .build();

            activities.add(act);
        }

        return activities;
    }

    @Bean
    public Collection<Activity> activitiesCollection(Collection<ActivityDto> activityDtos) {
        return activityDtos.stream()
                .map(dto -> new Activity(dto.getCodeZbe(), dto.getCodeTapas(), dto.getCodeVot(), dto.getCodeMct(), dto.getCodePriority()))
                .toList();
    }

    /**
     * Creates a PersonGroups object based on the provided collection of PersonCodeDto objects.
     *
     * @param personCodeDtos A collection of PersonCodeDto objects containing information about each person group.
     * @return The created PersonGroups object.
     */
    @Bean
    public PersonGroups personGroups(Collection<PersonCodeDto> personCodeDtos){

        PersonGroups.PersonGroupsBuilder personGroupsBuilder = PersonGroups.builder();

        for(PersonCodeDto personCodeDto : personCodeDtos) {
            TPS_PersonGroup personGroup = TPS_PersonGroup.builder()
                    .description(personCodeDto.getDescription())
                    .code(personCodeDto.getCode())
                    .personType(TPS_PersonType.valueOf(personCodeDto.getPersonType()))
                    .carCode(TPS_CarCode.getEnum(personCodeDto.getCodeCars()))
                    .hasChildCode(TPS_HasChildCode.valueOf(personCodeDto.getHasChild()))
                    .minAge(personCodeDto.getMinAge())
                    .maxAge(personCodeDto.getMaxAge())
                    .workStatus(TPS_WorkStatus.valueOf(personCodeDto.getWorkStatus()))
                    .sex(TPS_Sex.getEnum(personCodeDto.getCodeSex()))
                    .build();

            personGroupsBuilder.personGroup(personGroup.getCode(), personGroup);
        }

        return personGroupsBuilder.build();
    }

    @Bean
    public Collection<ActivityDto> activityDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.activities(), ActivityDto.class, ActivityDto::new);
    }

    @Bean
    public Collection<SchemeDto> schemeDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.schemes(), SchemeDto.class, SchemeDto::new);
    }

    @Bean
    public Collection<SchemeClassDto> schemeClassDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.schemeClasses(), SchemeClassDto.class, SchemeClassDto::new);
    }

    @Bean
    public Collection<EpisodeDto> episodeDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.episodes(), EpisodeDto.class, EpisodeDto::new);
    }

    @Bean
    public Collection<SchemeClassDistributionDto> schemeClassDistributionDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.schemeClassDistributions(), SchemeClassDistributionDto.class, SchemeClassDistributionDto::new);
    }

    @Bean
    public Collection<PersonCodeDto> personCodeDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.personGroups(), PersonCodeDto.class, PersonCodeDto::new);
    }

    @Bean(name = "homeActivityId")
    public int homeActivityId(SchemeProviderConfiguration configuration){
        return configuration.homeActivityCode();
    }
}
