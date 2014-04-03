/*
 * Copyright (C) 2007 Marc Hoffman <mmh@pleasantst.com>
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#ifndef AVUTIL_BFIN_ATTRIBUTES_H
#define AVUTIL_BFIN_ATTRIBUTES_H

#include "config.h"

#if defined(__FDPIC__) && CONFIG_SRAM
#define attribute_l1_text   __attribute__((l1_text))
#define attribute_l1_data_b __attribute__((l1_data_B))
#else
#define attribute_l1_text
#define attribute_l1_data_b
#endif

#endif /* AVUTIL_BFIN_ATTRIBUTES_H */
