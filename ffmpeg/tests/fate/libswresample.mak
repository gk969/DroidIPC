CROSS_TEST = $(foreach I,$(1),                                        \
                 $(foreach J,$(1),                                    \
                     $(if $(filter-out $(I),$(J)),                    \
                         $(eval $(call $(2),$(I),$(J),$(3),$(4),$(5))),    \
                     )))


SAMPLERATES = 2626 8000 44100 48000 96000

define ARESAMPLE
FATE_SWR_RESAMPLE += fate-swr-resample-$(3)-$(1)-$(2)
fate-swr-resample-$(3)-$(1)-$(2): tests/data/asynth-$(1)-1.wav
fate-swr-resample-$(3)-$(1)-$(2): CMD = ffmpeg -i $(TARGET_PATH)/tests/data/asynth-$(1)-1.wav -af atrim=end_sample=10240,aresample=$(2):internal_sample_fmt=$(3),aformat=$(3),aresample=$(1):internal_sample_fmt=$(3) -f wav -acodec pcm_s16le -

fate-swr-resample-$(3)-$(1)-$(2): CMP = stddev
fate-swr-resample-$(3)-$(1)-$(2): CMP_UNIT = $(5)
fate-swr-resample-$(3)-$(1)-$(2): FUZZ = 0.1
fate-swr-resample-$(3)-$(1)-$(2): REF = tests/data/asynth-$(1)-1.wav

#below list is generated by:
#you can use this if you need to update it!
#make -k  `make fate-list | grep swr` | egrep 'TEST|stddev' | tr '\n' '@' | sed 's#TEST *\([^@]*\)@stddev: *\([0-9.]*\)[^b@]*bytes: *\([0-9]*\) */ *\([0-9]*\)@#fate-\1: CMP_TARGET = \2@fate-\1: SIZE_TOLERANCE = \3 - \4@@#g' | tr '@' '\n'

fate-swr-resample-dblp-2626-44100: CMP_TARGET = 1393.01
fate-swr-resample-dblp-2626-44100: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-dblp-2626-48000: CMP_TARGET = 1393.01
fate-swr-resample-dblp-2626-48000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-dblp-2626-8000: CMP_TARGET = 1393.90
fate-swr-resample-dblp-2626-8000: SIZE_TOLERANCE = 31512 - 20482

fate-swr-resample-dblp-2626-96000: CMP_TARGET = 1393.01
fate-swr-resample-dblp-2626-96000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-dblp-44100-2626: CMP_TARGET = 185.84
fate-swr-resample-dblp-44100-2626: SIZE_TOLERANCE = 529200 - 20490

fate-swr-resample-dblp-44100-48000: CMP_TARGET = 9.70
fate-swr-resample-dblp-44100-48000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-dblp-44100-8000: CMP_TARGET = 75.46
fate-swr-resample-dblp-44100-8000: SIZE_TOLERANCE = 529200 - 20486

fate-swr-resample-dblp-44100-96000: CMP_TARGET = 11.47
fate-swr-resample-dblp-44100-96000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-dblp-48000-2626: CMP_TARGET = 456.55
fate-swr-resample-dblp-48000-2626: SIZE_TOLERANCE = 576000 - 20510

fate-swr-resample-dblp-48000-44100: CMP_TARGET = 1.16
fate-swr-resample-dblp-48000-44100: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-dblp-48000-8000: CMP_TARGET = 62.41
fate-swr-resample-dblp-48000-8000: SIZE_TOLERANCE = 576000 - 20484

fate-swr-resample-dblp-48000-96000: CMP_TARGET = 0.47
fate-swr-resample-dblp-48000-96000: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-dblp-8000-2626: CMP_TARGET = 2506.01
fate-swr-resample-dblp-8000-2626: SIZE_TOLERANCE = 96000 - 20486

fate-swr-resample-dblp-8000-44100: CMP_TARGET = 15.09
fate-swr-resample-dblp-8000-44100: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-dblp-8000-48000: CMP_TARGET = 14.68
fate-swr-resample-dblp-8000-48000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-dblp-8000-96000: CMP_TARGET = 13.82
fate-swr-resample-dblp-8000-96000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-dblp-96000-2626: CMP_TARGET = 675.14
fate-swr-resample-dblp-96000-2626: SIZE_TOLERANCE = 1152000 - 20474

fate-swr-resample-dblp-96000-44100: CMP_TARGET = 1.58
fate-swr-resample-dblp-96000-44100: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-dblp-96000-48000: CMP_TARGET = 1.04
fate-swr-resample-dblp-96000-48000: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-dblp-96000-8000: CMP_TARGET = 58.60
fate-swr-resample-dblp-96000-8000: SIZE_TOLERANCE = 1152000 - 20496

fate-swr-resample-fltp-2626-44100: CMP_TARGET = 1393.01
fate-swr-resample-fltp-2626-44100: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-fltp-2626-48000: CMP_TARGET = 1393.01
fate-swr-resample-fltp-2626-48000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-fltp-2626-8000: CMP_TARGET = 1393.90
fate-swr-resample-fltp-2626-8000: SIZE_TOLERANCE = 31512 - 20482

fate-swr-resample-fltp-2626-96000: CMP_TARGET = 1393.01
fate-swr-resample-fltp-2626-96000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-fltp-44100-2626: CMP_TARGET = 185.84
fate-swr-resample-fltp-44100-2626: SIZE_TOLERANCE = 529200 - 20490

fate-swr-resample-fltp-44100-48000: CMP_TARGET = 9.70
fate-swr-resample-fltp-44100-48000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-fltp-44100-8000: CMP_TARGET = 75.46
fate-swr-resample-fltp-44100-8000: SIZE_TOLERANCE = 529200 - 20486

fate-swr-resample-fltp-44100-96000: CMP_TARGET = 11.47
fate-swr-resample-fltp-44100-96000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-fltp-48000-2626: CMP_TARGET = 456.55
fate-swr-resample-fltp-48000-2626: SIZE_TOLERANCE = 576000 - 20510

fate-swr-resample-fltp-48000-44100: CMP_TARGET = 1.16
fate-swr-resample-fltp-48000-44100: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-fltp-48000-8000: CMP_TARGET = 62.41
fate-swr-resample-fltp-48000-8000: SIZE_TOLERANCE = 576000 - 20484

fate-swr-resample-fltp-48000-96000: CMP_TARGET = 0.47
fate-swr-resample-fltp-48000-96000: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-fltp-8000-2626: CMP_TARGET = 2506.01
fate-swr-resample-fltp-8000-2626: SIZE_TOLERANCE = 96000 - 20486

fate-swr-resample-fltp-8000-44100: CMP_TARGET = 15.09
fate-swr-resample-fltp-8000-44100: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-fltp-8000-48000: CMP_TARGET = 14.68
fate-swr-resample-fltp-8000-48000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-fltp-8000-96000: CMP_TARGET = 13.82
fate-swr-resample-fltp-8000-96000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-fltp-96000-2626: CMP_TARGET = 675.14
fate-swr-resample-fltp-96000-2626: SIZE_TOLERANCE = 1152000 - 20474

fate-swr-resample-fltp-96000-44100: CMP_TARGET = 1.58
fate-swr-resample-fltp-96000-44100: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-fltp-96000-48000: CMP_TARGET = 1.04
fate-swr-resample-fltp-96000-48000: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-fltp-96000-8000: CMP_TARGET = 58.60
fate-swr-resample-fltp-96000-8000: SIZE_TOLERANCE = 1152000 - 20496

fate-swr-resample-s16p-2626-44100: CMP_TARGET = 1393.01
fate-swr-resample-s16p-2626-44100: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-s16p-2626-48000: CMP_TARGET = 1392.99
fate-swr-resample-s16p-2626-48000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-s16p-2626-8000: CMP_TARGET = 1393.90
fate-swr-resample-s16p-2626-8000: SIZE_TOLERANCE = 31512 - 20482

fate-swr-resample-s16p-2626-96000: CMP_TARGET = 1393.08
fate-swr-resample-s16p-2626-96000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-s16p-44100-2626: CMP_TARGET = 185.84
fate-swr-resample-s16p-44100-2626: SIZE_TOLERANCE = 529200 - 20490

fate-swr-resample-s16p-44100-48000: CMP_TARGET = 9.71
fate-swr-resample-s16p-44100-48000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-s16p-44100-8000: CMP_TARGET = 75.46
fate-swr-resample-s16p-44100-8000: SIZE_TOLERANCE = 529200 - 20486

fate-swr-resample-s16p-44100-96000: CMP_TARGET = 11.48
fate-swr-resample-s16p-44100-96000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-s16p-48000-2626: CMP_TARGET = 456.55
fate-swr-resample-s16p-48000-2626: SIZE_TOLERANCE = 576000 - 20510

fate-swr-resample-s16p-48000-44100: CMP_TARGET = 1.22
fate-swr-resample-s16p-48000-44100: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-s16p-48000-8000: CMP_TARGET = 62.41
fate-swr-resample-s16p-48000-8000: SIZE_TOLERANCE = 576000 - 20484

fate-swr-resample-s16p-48000-96000: CMP_TARGET = 0.50
fate-swr-resample-s16p-48000-96000: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-s16p-8000-2626: CMP_TARGET = 2506.02
fate-swr-resample-s16p-8000-2626: SIZE_TOLERANCE = 96000 - 20486

fate-swr-resample-s16p-8000-44100: CMP_TARGET = 15.12
fate-swr-resample-s16p-8000-44100: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-s16p-8000-48000: CMP_TARGET = 14.69
fate-swr-resample-s16p-8000-48000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-s16p-8000-96000: CMP_TARGET = 13.83
fate-swr-resample-s16p-8000-96000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-s16p-96000-2626: CMP_TARGET = 675.14
fate-swr-resample-s16p-96000-2626: SIZE_TOLERANCE = 1152000 - 20474

fate-swr-resample-s16p-96000-44100: CMP_TARGET = 1.62
fate-swr-resample-s16p-96000-44100: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-s16p-96000-48000: CMP_TARGET = 1.03
fate-swr-resample-s16p-96000-48000: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-s16p-96000-8000: CMP_TARGET = 58.60
fate-swr-resample-s16p-96000-8000: SIZE_TOLERANCE = 1152000 - 20496

fate-swr-resample-s32p-2626-44100: CMP_TARGET = 1393.01
fate-swr-resample-s32p-2626-44100: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-s32p-2626-48000: CMP_TARGET = 1393.01
fate-swr-resample-s32p-2626-48000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-s32p-2626-8000: CMP_TARGET = 1393.90
fate-swr-resample-s32p-2626-8000: SIZE_TOLERANCE = 31512 - 20482

fate-swr-resample-s32p-2626-96000: CMP_TARGET = 1393.01
fate-swr-resample-s32p-2626-96000: SIZE_TOLERANCE = 31512 - 20480

fate-swr-resample-s32p-44100-2626: CMP_TARGET = 185.84
fate-swr-resample-s32p-44100-2626: SIZE_TOLERANCE = 529200 - 20490

fate-swr-resample-s32p-44100-48000: CMP_TARGET = 9.70
fate-swr-resample-s32p-44100-48000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-s32p-44100-8000: CMP_TARGET = 75.46
fate-swr-resample-s32p-44100-8000: SIZE_TOLERANCE = 529200 - 20486

fate-swr-resample-s32p-44100-96000: CMP_TARGET = 11.47
fate-swr-resample-s32p-44100-96000: SIZE_TOLERANCE = 529200 - 20482

fate-swr-resample-s32p-48000-2626: CMP_TARGET = 456.55
fate-swr-resample-s32p-48000-2626: SIZE_TOLERANCE = 576000 - 20510

fate-swr-resample-s32p-48000-44100: CMP_TARGET = 1.16
fate-swr-resample-s32p-48000-44100: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-s32p-48000-8000: CMP_TARGET = 62.41
fate-swr-resample-s32p-48000-8000: SIZE_TOLERANCE = 576000 - 20484

fate-swr-resample-s32p-48000-96000: CMP_TARGET = 0.47
fate-swr-resample-s32p-48000-96000: SIZE_TOLERANCE = 576000 - 20480

fate-swr-resample-s32p-8000-2626: CMP_TARGET = 2506.01
fate-swr-resample-s32p-8000-2626: SIZE_TOLERANCE = 96000 - 20486

fate-swr-resample-s32p-8000-44100: CMP_TARGET = 15.09
fate-swr-resample-s32p-8000-44100: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-s32p-8000-48000: CMP_TARGET = 14.68
fate-swr-resample-s32p-8000-48000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-s32p-8000-96000: CMP_TARGET = 13.82
fate-swr-resample-s32p-8000-96000: SIZE_TOLERANCE = 96000 - 20480

fate-swr-resample-s32p-96000-2626: CMP_TARGET = 675.14
fate-swr-resample-s32p-96000-2626: SIZE_TOLERANCE = 1152000 - 20474

fate-swr-resample-s32p-96000-44100: CMP_TARGET = 1.58
fate-swr-resample-s32p-96000-44100: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-s32p-96000-48000: CMP_TARGET = 1.04
fate-swr-resample-s32p-96000-48000: SIZE_TOLERANCE = 1152000 - 20480

fate-swr-resample-s32p-96000-8000: CMP_TARGET = 58.60
fate-swr-resample-s32p-96000-8000: SIZE_TOLERANCE = 1152000 - 20496
endef


$(call CROSS_TEST,$(SAMPLERATES),ARESAMPLE,s16p,s16le,s16)
$(call CROSS_TEST,$(SAMPLERATES),ARESAMPLE,s32p,s32le,s16)
$(call CROSS_TEST,$(SAMPLERATES),ARESAMPLE,fltp,f32le,s16)
$(call CROSS_TEST,$(SAMPLERATES),ARESAMPLE,dblp,f64le,s16)

FATE_SWR_RESAMPLE-$(call FILTERDEMDECENCMUX, ARESAMPLE, WAV, PCM_S16LE, PCM_S16LE, WAV) += $(FATE_SWR_RESAMPLE)
fate-swr-resample: $(FATE_SWR_RESAMPLE-yes)
FATE_SWR += $(FATE_SWR_RESAMPLE-yes)

FATE_FFMPEG += $(FATE_SWR)
fate-swr: $(FATE_SWR)
