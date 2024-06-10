package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.modechoice.ModesConfiguration;
import de.dlr.ivf.tapas.configuration.json.modechoice.SimpleModeParameters;
import de.dlr.ivf.tapas.dto.ModeDto;
import de.dlr.ivf.tapas.model.constants.TPS_InternalConstant;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.EnumMap;

@Configuration
public class ModesFactory {

    private final TPS_DB_IO dbIo;
    private final ModesConfiguration configuration;

    @Autowired
    public ModesFactory(TPS_DB_IO dbIo, ModesConfiguration configuration) {
        this.dbIo = dbIo;
        this.configuration = configuration;
    }

    @Bean
    public Modes modes(Collection<ModeDto> modeDtos, EnumMap<TPS_Mode.ModeType, SimpleModeParameters> modeParams){

        Modes.ModesBuilder modesBuilder = Modes.builder();
        for(ModeDto modeDto : modeDtos){

            TPS_Mode.ModeType modeType = TPS_Mode.ModeType.valueOf(modeDto.getName());

            //initialize with data from database
            TPS_Mode.TPS_ModeBuilder modeBuilder = TPS_Mode.builder()
                    .name(modeDto.getName())
                    .id(modeDto.getCodeMct())
                    .isFix(modeDto.isFix())
                    .modeType(modeType)
                    .internalConstant(new TPS_InternalConstant<>(modeDto.getNameMct(), modeDto.getCodeMct(),
                            TPS_Mode.TPS_ModeCodeType.valueOf(modeDto.getTypeMct())))
                    .internalConstant(new TPS_InternalConstant<>(modeDto.getNameVot(), modeDto.getCodeVot(),
                            TPS_Mode.TPS_ModeCodeType.valueOf(modeDto.getTypeVot())));

            //initialize with data from parameterClass
            SimpleModeParameters modeParameters = modeParams.get(modeType);

            TPS_Mode mode = modeBuilder.beelineFactor(modeParameters.beelineFactor())
                    .velocity(modeParameters.velocity())
                    .variableCostPerKm(modeParameters.variableCostPerKm())
                    .variableCostPerKmBase(modeParameters.variableCostPerKmBase())
                    .costPerKm(modeParameters.costPerKm())
                    .costPerKmBase(modeParameters.costPerKmBase())
                    .useBase(modeParameters.useBase())
                    .build();
            modesBuilder.addModeById(mode.getId(), mode);
            modesBuilder.addModeByName(mode.getName(), mode);
        }
        return modesBuilder.build();
    }

    @Bean
    public EnumMap<TPS_Mode.ModeType, SimpleModeParameters> getModeParameters(){

        EnumMap<TPS_Mode.ModeType, SimpleModeParameters> modeParams = new EnumMap<>(TPS_Mode.ModeType.class);
        for (SimpleModeParameters modeParameter : configuration.modeParameters()) {
            TPS_Mode.ModeType modeType = TPS_Mode.ModeType.valueOf(modeParameter.name());
            modeParams.put(modeType, modeParameter);
        }

        return modeParams;
    }

    @Bean
    public Collection<ModeDto> modeDtos(){
        return dbIo.readFromDb(configuration.modes(), ModeDto.class, ModeDto::new);
    }
}
